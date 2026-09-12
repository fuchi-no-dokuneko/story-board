package dev.storyblock.contracts;

import java.util.Map;

final class CanonicalRevisionRequireString {
    static String requireString(Map<String, Object> object, String field, String path) {
        Object value = object.get(field);
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(path + "." + field + " must be a string");
        }
        return string;
    }
}
