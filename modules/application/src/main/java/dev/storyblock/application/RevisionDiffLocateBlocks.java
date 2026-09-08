package dev.storyblock.application;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.Map;
import java.util.TreeMap;
import static dev.storyblock.application.RevisionDiff.LocatedBlock;

final class RevisionDiffLocateBlocks {
    static Map<String, LocatedBlock> locateBlocks(RevisionManifest revision) {
        Map<String, LocatedBlock> located = new TreeMap<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                for (int index = 0; index < scene.blocks().size(); index++) {
                    NarrativeBlock block = scene.blocks().get(index);
                    located.put(block.id().value(), new LocatedBlock(scene.id(), index, block));
                }
            }
        }
        return located;
    }
}
