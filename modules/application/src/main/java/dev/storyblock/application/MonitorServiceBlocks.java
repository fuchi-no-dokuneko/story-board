package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.LinkedHashMap;
import java.util.Map;

final class MonitorServiceBlocks {
    static Map<Ids.BlockId, NarrativeBlock> blocks(RevisionManifest revision) {
        Map<Ids.BlockId, NarrativeBlock> result = new LinkedHashMap<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                for (NarrativeBlock block : scene.blocks()) {
                    result.put(block.id(), block);
                }
            }
        }
        return Map.copyOf(result);
    }
}
