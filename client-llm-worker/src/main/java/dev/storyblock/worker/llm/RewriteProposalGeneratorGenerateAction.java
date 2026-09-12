package dev.storyblock.worker.llm;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteCandidateBlock;
import dev.storyblock.rewrite.RewriteModelResponse;
import dev.storyblock.rewrite.RewriteSourceBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import dev.storyblock.rewrite.RewriteWorkerInput;
import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class RewriteProposalGeneratorGenerateAction {
    static RewriteTextProposal generate(RewriteProposalGenerator self, RewriteWorkerInput input, Instant createdAt) throws IOException, InterruptedException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(createdAt, "createdAt");
        byte[] responseBytes = self.transport.invoke(CanonicalJson.bytes(self.request(input)));
        RewriteModelResponse response = RewriteProposalGeneratorParseResponse.parseResponse(responseBytes);
        if (!self.modelId.equals(response.modelId())
                || !input.inputHash().equals(response.inputHash())) {
            throw new LlmWorkerProtocolException(
                    "Model response does not match the requested model and input"
            );
        }
        Map<Ids.BlockId, RewriteSourceBlock> sources = new HashMap<>();
        Map<Ids.BlockId, Integer> sourceOrdinals = new HashMap<>();
        input.blocks().forEach(block -> sources.put(block.blockId(), block));
        for (int index = 0; index < input.blocks().size(); index++) {
            sourceOrdinals.put(input.blocks().get(index).blockId(), index);
        }
        List<RewriteCandidateBlock> candidates = response.replacements().stream()
                .map(replacement -> self.bind(replacement, sources))
                .sorted(Comparator.comparingInt(candidate -> sourceOrdinals.get(
                        candidate.blockId()
                )))
                .toList();
        try {
            return new RewriteTextProposal(
                    input,
                    self.modelId,
                    CanonicalJson.hash(response.canonicalValue()),
                    candidates,
                    createdAt
            );
        } catch (RuntimeException invalid) {
            throw new LlmWorkerProtocolException(
                    "Model response violates the bounded rewrite contract"
            );
        }
    }
}
