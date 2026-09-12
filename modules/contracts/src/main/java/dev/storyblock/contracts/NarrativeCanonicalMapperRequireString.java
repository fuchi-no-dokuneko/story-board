package dev.storyblock.contracts;

import java.util.Map;

final class NarrativeCanonicalMapperRequireString {
    static String requireString(Map<String, Object> object, String field) {
        Object value = object.get(field);
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(field + " must be a string");
        }
        return string;
    }
}
