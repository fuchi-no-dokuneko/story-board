package dev.storyblock.application;

import java.util.Map;

final class NovelCatalogServiceIntExtension {
    static int intExtension(Map<String, Object> extensions, String key) {
        Object value = extensions.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }
}
