package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalPackageException;
import java.util.Map;
import java.util.Set;

final class CanonicalTransferControllerRequireKeys {
    static void requireKeys(
            Map<String, Object> value,
            Set<String> expected,
            String path
    ) {
        for (String field : expected) {
            if (!value.containsKey(field)) {
                throw new CanonicalPackageException(path + " is missing " + field);
            }
        }
        for (String field : value.keySet()) {
            if (!expected.contains(field)) {
                throw new CanonicalPackageException(path + " contains unknown field " + field);
            }
        }
    }
}
