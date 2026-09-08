package dev.storyblock.contracts;

import java.util.Map;

final class NarrativeCanonicalMapperOptionalString {
    static String optionalString(Map<String, Object> object, String field) {
        return object.containsKey(field) ? NarrativeCanonicalMapperRequireString.requireString(object, field) : null;
    }
}
