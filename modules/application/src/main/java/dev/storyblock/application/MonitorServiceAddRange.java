package dev.storyblock.application;

import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.Ids;
import java.util.Set;

final class MonitorServiceAddRange {
    static void addRange(Set<Ids.BlockId> result, BlockRangeGuard range) {
        result.addAll(MonitorServiceRangeIds.rangeIds(range));
        MonitorServiceAdd.add(result, range.expectedPreviousBlockId());
        MonitorServiceAdd.add(result, range.expectedNextBlockId());
    }
}
