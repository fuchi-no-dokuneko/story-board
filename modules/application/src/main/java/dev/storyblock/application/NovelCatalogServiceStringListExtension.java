package dev.storyblock.application;

import java.util.List;
import java.util.Map;

final class NovelCatalogServiceStringListExtension {
    static List<String> stringListExtension(
            Map<String, Object> extensions,
            String key
    ) {
        Object value = extensions.get(key);
        if (!(value instanceof List<?> entries)) {
            return List.of();
        }
        return entries.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }
}
