package dev.storyblock.monitor;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.monitor.MonitorPacketFactory.BlockLocation;

final class MonitorPacketFactoryFlatten {
    static List<BlockLocation> flatten(RevisionManifest revision) {
        List<BlockLocation> result = new ArrayList<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                for (NarrativeBlock block : scene.blocks()) {
                    result.add(new BlockLocation(scene.id(), block));
                }
            }
        }
        return List.copyOf(result);
    }
}
