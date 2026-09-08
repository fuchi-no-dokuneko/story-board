package dev.storyblock.worker.llm;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteCandidateBlock;
import dev.storyblock.rewrite.RewriteModelReplacement;
import dev.storyblock.rewrite.RewriteSourceBlock;
import java.util.Map;

final class RewriteProposalGeneratorBindAction {
    static RewriteCandidateBlock bind(RewriteProposalGenerator self, RewriteModelReplacement replacement, Map<Ids.BlockId, RewriteSourceBlock> sources)  {
        RewriteSourceBlock source = sources.get(replacement.blockId());
        if (source == null || !source.editable()) {
            throw new LlmWorkerProtocolException(
                    "Model response targets a block outside the editable range"
            );
        }
        try {
            return RewriteCandidateBlock.create(source, replacement.text());
        } catch (RuntimeException invalid) {
            throw new LlmWorkerProtocolException(
                    "Model response contains an invalid replacement"
            );
        }
    }
}
