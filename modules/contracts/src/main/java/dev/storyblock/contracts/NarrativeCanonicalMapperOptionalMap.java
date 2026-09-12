package dev.storyblock.contracts;

import java.util.Map;

final class NarrativeCanonicalMapperOptionalMap {
    static Map<String, Object> optionalMap(Object value, String path) {
        return value == null ? Map.of() : NarrativeCanonicalMapper.requireMap(value, path);
    }
}
