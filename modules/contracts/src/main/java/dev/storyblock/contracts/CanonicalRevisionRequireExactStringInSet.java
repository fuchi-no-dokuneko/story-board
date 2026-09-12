package dev.storyblock.contracts;

import java.util.Map;
import java.util.Set;

final class CanonicalRevisionRequireExactStringInSet {
    static void requireExactStringInSet(
            Map<String, Object> object,
            String field,
            Set<String> allowed,
            String path
    ) {
        String actual = CanonicalRevisionRequireString.requireString(object, field, path);
        if (!allowed.contains(actual)) {
            throw new IllegalArgumentException(path + "." + field + " has an unsupported value");
        }
    }
}
