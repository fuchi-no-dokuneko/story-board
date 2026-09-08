package dev.storyblock.contracts;

import java.util.Map;

final class NarrativeCanonicalMapperPutOptional {
    static void putOptional(Map<String, Object> target, String field, Object value) {
        if (value != null) {
            target.put(field, value);
        }
    }
}
