package dev.storyblock.application;

import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import java.util.Objects;

final class NovelCatalogServiceTitle {
    static String title(NarrativeNovel novel) {
        Object configured = novel.extensions().get("title");
        if (configured instanceof String title && !title.isBlank()) {
            return title;
        }
        return novel.chapters().stream()
                .map(NarrativeChapter::title)
                .filter(Objects::nonNull)
                .filter(title -> !title.isBlank())
                .findFirst()
                .orElse(novel.id().value());
    }
}
