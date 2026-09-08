package dev.storyblock.renderer;

import dev.storyblock.domain.RevisionManifest;

final class DeterministicPdfRendererTitle {
    static String title(RevisionManifest revision) {
        Object value = revision.novel().extensions().get("title");
        return value instanceof String text && !text.isBlank()
                ? text.strip()
                : "StoryBlock Novel";
    }
}
