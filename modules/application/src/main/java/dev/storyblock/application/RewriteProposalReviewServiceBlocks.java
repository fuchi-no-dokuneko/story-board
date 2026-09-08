package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.storage.StoredRevision;
import java.util.HashMap;
import java.util.Map;

final class RewriteProposalReviewServiceBlocks {
    static Map<Ids.BlockId, NarrativeBlock> blocks(StoredRevision revision) {
        Map<Ids.BlockId, NarrativeBlock> result = new HashMap<>();
        revision.manifest().novel().chapters().forEach(chapter ->
                chapter.scenes().forEach(scene ->
                        scene.blocks().forEach(block -> result.put(block.id(), block))
                )
        );
        return result;
    }
}
