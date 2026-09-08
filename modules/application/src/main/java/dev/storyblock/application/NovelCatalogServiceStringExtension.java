package dev.storyblock.application;

import java.util.Map;

final class NovelCatalogServiceStringExtension {
    static String stringExtension(
            Map<String, Object> extensions,
            String key,
            String fallback
    ) {
        Object value = extensions.get(key);
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }
}
