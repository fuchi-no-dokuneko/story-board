package dev.storyblock.contracts;

import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperRequireKeys {
    static void requireKeys(
            Map<String, Object> value,
            Set<String> required,
            String path
    ) {
        requireKeys(value, required, Set.of(), path);
    }

    static void requireKeys(
            Map<String, Object> value,
            Set<String> required,
            Set<String> optional,
            String path
    ) {
        for (String field : required) {
            if (!value.containsKey(field)) {
                throw new IllegalArgumentException(path + " is missing " + field);
            }
        }
        for (String field : value.keySet()) {
            if (!required.contains(field) && !optional.contains(field)) {
                throw new IllegalArgumentException(path + " contains unknown field " + field);
            }
        }
    }
}
