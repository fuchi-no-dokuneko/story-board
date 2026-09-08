package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.util.Set;

final class MonitorServiceDirectlyChangedBlockIds {
    static Set<Ids.BlockId> directlyChangedBlockIds(EditOperation operation) {
        if (operation instanceof EditOperation.ReplaceBlockRange value) {
            return MonitorServiceRangeIds.rangeIds(value.range());
        }
        if (operation instanceof EditOperation.DeleteBlockRange value) {
            return MonitorServiceRangeIds.rangeIds(value.range());
        }
        if (operation instanceof EditOperation.SplitBlock value) {
            return MonitorServiceRangeIds.rangeIds(value.block());
        }
        if (operation instanceof EditOperation.MergeBlocks value) {
            return MonitorServiceRangeIds.rangeIds(value.range());
        }
        if (operation instanceof EditOperation.ExtendBlock value) {
            return MonitorServiceRangeIds.rangeIds(value.block());
        }
        if (operation instanceof EditOperation.MoveBlockRange value) {
            return MonitorServiceRangeIds.rangeIds(value.range());
        }
        if (operation instanceof EditOperation.CorrectBlockMeta value) {
            return Set.of(value.block().blockId());
        }
        return Set.of();
    }
}
