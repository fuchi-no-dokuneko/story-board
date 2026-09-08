package dev.storyblock.detector;

import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;

final class AdjacentMetadataDetectorScenes {
    static List<NarrativeScene> scenes(RevisionManifest revision) {
        List<NarrativeScene> result = new ArrayList<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            result.addAll(chapter.scenes());
        }
        return List.copyOf(result);
    }
}
