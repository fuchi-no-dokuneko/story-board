package dev.storyblock.application;

import dev.storyblock.domain.*;
import java.util.List;

final class RewriteVisibleRange {
    static BlockRangeGuard require(RevisionManifest revision, List<Ids.BlockId> affected) {
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                var visible = NarrativeView.visibleBlocks(scene);
                if (visible.stream().noneMatch(block -> block.id().equals(affected.getFirst()))) continue;
                if (!visible.stream().map(NarrativeBlock::id).toList().containsAll(affected)) {
                    throw BlockHidden.unsupported("Rewrite range must remain inside one visible scene");
                }
                var guard = BlockRangeGuard.capture(scene, affected.getFirst(), affected.getLast());
                var range = EditOperationValidator.validateRange(revision, guard);
                if (range.blocks().stream().anyMatch(NarrativeBlock::isHidden)) {
                    throw BlockHidden.unsupported("Rewrite range contains a hidden block");
                }
                return guard;
            }
        }
        throw BlockHidden.unsupported("Rewrite range has no visible first block");
    }
}
