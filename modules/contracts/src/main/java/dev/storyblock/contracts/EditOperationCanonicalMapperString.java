package dev.storyblock.contracts;

import java.util.Map;

final class EditOperationCanonicalMapperString {
    static String string(Map<String, Object> value, String field, String path) {
        Object entry = value.get(field);
        if (!(entry instanceof String string)) {
            throw new IllegalArgumentException(path + "." + field + " must be a string");
        }
        return string;
    }
}
