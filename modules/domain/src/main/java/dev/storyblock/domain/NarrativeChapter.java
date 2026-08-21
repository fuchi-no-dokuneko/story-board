package dev.storyblock.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record NarrativeChapter(
        Ids.ChapterId id,
        OrderKey orderKey,
        String title,
        List<NarrativeScene> scenes,
        Map<String, Object> extensions
) {
    public NarrativeChapter {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(orderKey, "orderKey");
        scenes = List.copyOf(scenes);
        extensions = CanonicalValues.freezeMap(extensions, "chapter.extensions");
        validateScenes(id, scenes);
    }

    public NarrativeChapter withScenes(List<NarrativeScene> newScenes) {
        return new NarrativeChapter(id, orderKey, title, newScenes, extensions);
    }

    private static void validateScenes(Ids.ChapterId chapterId, List<NarrativeScene> scenes) {
        Set<Ids.SceneId> ids = new HashSet<>();
        OrderKey previous = null;
        for (NarrativeScene scene : scenes) {
            Objects.requireNonNull(scene, "chapter scene");
            if (!chapterId.equals(scene.chapterId())) {
                throw new IllegalArgumentException("Scene chapter_id does not match its parent chapter");
            }
            if (previous != null && previous.compareTo(scene.orderKey()) >= 0) {
                throw new IllegalArgumentException("Chapter scene order keys must be strictly increasing");
            }
            if (!ids.add(scene.id())) {
                throw new IllegalArgumentException("Chapter contains duplicate scene ID " + scene.id().value());
            }
            previous = scene.orderKey();
        }
    }
}
