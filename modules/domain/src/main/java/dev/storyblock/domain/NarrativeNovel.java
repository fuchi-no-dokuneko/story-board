package dev.storyblock.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record NarrativeNovel(
        Ids.NovelId id,
        List<NarrativeChapter> chapters,
        Map<String, Object> extensions
) {
    public NarrativeNovel {
        Objects.requireNonNull(id, "id");
        chapters = List.copyOf(chapters);
        extensions = CanonicalValues.freezeMap(extensions, "novel.extensions");
        validateChapters(chapters);
    }

    public NarrativeNovel withChapters(List<NarrativeChapter> newChapters) {
        return new NarrativeNovel(id, newChapters, extensions);
    }

    private static void validateChapters(List<NarrativeChapter> chapters) {
        Set<Ids.ChapterId> ids = new HashSet<>();
        OrderKey previous = null;
        for (NarrativeChapter chapter : chapters) {
            Objects.requireNonNull(chapter, "novel chapter");
            if (previous != null && previous.compareTo(chapter.orderKey()) >= 0) {
                throw new IllegalArgumentException("Novel chapter order keys must be strictly increasing");
            }
            if (!ids.add(chapter.id())) {
                throw new IllegalArgumentException("Novel contains duplicate chapter ID " + chapter.id().value());
            }
            previous = chapter.orderKey();
        }
    }
}
