package dev.storyblock.contracts;

import java.util.Map;

final class CanonicalRevisionOptionalString {
    static void optionalString(Map<String, Object> object, String field, String path) {
        if (object.containsKey(field)) {
            CanonicalRevisionRequireString.requireString(object, field, path);
        }
    }
}
