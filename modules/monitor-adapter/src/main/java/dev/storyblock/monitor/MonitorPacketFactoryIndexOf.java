package dev.storyblock.monitor;

import dev.storyblock.domain.Ids;
import java.util.List;
import static dev.storyblock.monitor.MonitorPacketFactory.BlockLocation;

final class MonitorPacketFactoryIndexOf {
    static int indexOf(List<BlockLocation> blocks, Ids.BlockId targetBlockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).block().id().equals(targetBlockId)) {
                return index;
            }
        }
        throw new IllegalArgumentException(
                "Revision does not contain monitor target " + targetBlockId.value()
        );
    }
}
