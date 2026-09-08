package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.SceneBoundaryContract;
import java.util.Set;

final class MonitorServiceAddBoundary {
    static void addBoundary(
            Set<Ids.BlockId> result,
            SceneBoundaryContract boundary
    ) {
        MonitorServiceAdd.add(result, boundary.firstBlockId());
        MonitorServiceAdd.add(result, boundary.lastBlockId());
    }
}
