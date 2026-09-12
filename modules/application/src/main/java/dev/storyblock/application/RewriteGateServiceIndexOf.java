package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.policy.RewriteEligibilityException;
import dev.storyblock.style.StyleAnalysisBlock;
import java.util.List;

final class RewriteGateServiceIndexOf {
    static int indexOf(
            List<StyleAnalysisBlock> blocks,
            Ids.BlockId blockId
    ) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).block().id().equals(blockId)) {
                return index;
            }
        }
        throw new RewriteEligibilityException(
                "Rewrite affected block is outside the analysis snapshot"
        );
    }
}
