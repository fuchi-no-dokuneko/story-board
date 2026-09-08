package dev.storyblock.domain;

import java.util.HashSet;
import java.util.Set;

final class EditOperationValidatorRangeIds {
    static Set<Ids.BlockId> rangeIds(BlockRangeGuard range) {
        Set<Ids.BlockId> ids = new HashSet<>();
        for (BlockVersionRef block : range.expectedBlocks()) {
            ids.add(block.blockId());
        }
        return Set.copyOf(ids);
    }
}
