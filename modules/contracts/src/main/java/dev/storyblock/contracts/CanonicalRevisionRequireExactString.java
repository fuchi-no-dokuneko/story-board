package dev.storyblock.contracts;

import java.util.Map;

final class CanonicalRevisionRequireExactString {
    static void requireExactString(
            Map<String, Object> object,
            String field,
            String expected,
            String path
    ) {
        String actual = CanonicalRevisionRequireString.requireString(object, field, path);
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(path + "." + field + " must equal " + expected);
        }
    }
}
