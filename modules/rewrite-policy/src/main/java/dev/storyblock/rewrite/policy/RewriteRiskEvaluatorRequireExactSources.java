package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.rewrite.RewriteSourceBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import java.util.List;

final class RewriteRiskEvaluatorRequireExactSources {
    static void requireExactSources(
            RewriteTextProposal proposal,
            List<NarrativeBlock> sourceBlocks
    ) {
        List<RewriteSourceBlock> expected = proposal.input().blocks();
        if (sourceBlocks.size() != expected.size()) {
            throw new RewriteRiskPolicyException(
                    "Rewrite source metadata does not cover the exact worker input"
            );
        }
        for (int index = 0; index < expected.size(); index++) {
            RewriteSourceBlock binding = expected.get(index);
            NarrativeBlock source = sourceBlocks.get(index);
            if (!source.id().equals(binding.blockId())
                    || !source.versionId().equals(binding.blockVersionId())
                    || !source.text().equals(binding.text())
                    || !CanonicalJson.hash(source.text()).equals(binding.textHash())) {
                throw new RewriteRiskPolicyException(
                        "Rewrite source metadata does not match immutable worker input"
                );
            }
        }
    }
}
