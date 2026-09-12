package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import java.util.Set;

final class MonitorServiceAdd {
    static void add(Set<Ids.BlockId> result, Ids.BlockId blockId) {
        if (blockId != null) {
            result.add(blockId);
        }
    }
}
