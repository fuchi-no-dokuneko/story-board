package dev.storyblock.application;

import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.Map;
import java.util.TreeMap;

final class RevisionDiffLocateScenes {
    static Map<String, NarrativeScene> locateScenes(RevisionManifest revision) {
        Map<String, NarrativeScene> located = new TreeMap<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                located.put(scene.id().value(), scene);
            }
        }
        return located;
    }
}
