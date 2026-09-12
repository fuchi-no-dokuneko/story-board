package dev.storyblock.contracts;

import java.util.Map;

final class NarrativeCanonicalMapperPutExtensions {
    static void putExtensions(Map<String, Object> target, Map<String, Object> extensions) {
        if (!extensions.isEmpty()) {
            target.put("extensions", extensions);
        }
    }
}
