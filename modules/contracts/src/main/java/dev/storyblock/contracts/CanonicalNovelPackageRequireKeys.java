package dev.storyblock.contracts;

import java.util.Map;
import java.util.Set;

final class CanonicalNovelPackageRequireKeys {
    static void requireKeys(
            Map<String, Object> value,
            Set<String> required,
            String path
    ) {
        for (String field : required) {
            if (!value.containsKey(field)) {
                throw new CanonicalPackageException(path + " is missing " + field);
            }
        }
        for (String field : value.keySet()) {
            if (!required.contains(field)) {
                throw new CanonicalPackageException(path + " contains unknown field " + field);
            }
        }
    }
}
