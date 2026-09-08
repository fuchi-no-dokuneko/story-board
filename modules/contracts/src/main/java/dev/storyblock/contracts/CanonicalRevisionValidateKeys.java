package dev.storyblock.contracts;

import java.util.Map;
import java.util.Set;

final class CanonicalRevisionValidateKeys {
    static void validateKeys(
            Map<String, Object> object,
            Set<String> required,
            Set<String> optional,
            String path
    ) {
        for (String key : required) {
            if (!object.containsKey(key)) {
                throw new IllegalArgumentException(path + " is missing required field " + key);
            }
        }
        for (String key : object.keySet()) {
            if (!required.contains(key) && !optional.contains(key)) {
                throw new IllegalArgumentException(path + " contains unknown field " + key);
            }
        }
    }
}
