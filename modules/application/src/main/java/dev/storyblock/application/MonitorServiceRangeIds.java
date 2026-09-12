package dev.storyblock.application;

import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.Ids;
import java.util.Set;

final class MonitorServiceRangeIds {
    static Set<Ids.BlockId> rangeIds(BlockRangeGuard range) {
        return range.expectedBlocks().stream()
                .map(reference -> reference.blockId())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
