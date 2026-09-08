package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.rewrite.RewriteTextProposal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RewriteProposalReviewServiceExactInputBlocks {
    static List<NarrativeBlock> exactInputBlocks(
            RewriteTextProposal proposal,
            List<NarrativeBlock> snapshot
    ) {
        Map<Ids.BlockId, NarrativeBlock> values = new HashMap<>();
        snapshot.forEach(block -> values.put(block.id(), block));
        return proposal.input().blocks().stream().map(binding -> {
            NarrativeBlock block = values.get(binding.blockId());
            if (block == null) {
                throw new IllegalArgumentException(
                        "Rewrite input block is outside its analysis snapshot"
                );
            }
            return block;
        }).toList();
    }
}
