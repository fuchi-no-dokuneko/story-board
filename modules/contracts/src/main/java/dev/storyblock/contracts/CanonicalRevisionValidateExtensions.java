package dev.storyblock.contracts;

import java.util.Map;
import static dev.storyblock.contracts.CanonicalRevision.EXTENSION_NAME;

final class CanonicalRevisionValidateExtensions {
    static void validateExtensions(Object value, String path) {
        if (value == null) {
            return;
        }
        Map<String, Object> extensions = CanonicalRevision.requireMap(value, path);
        for (String name : extensions.keySet()) {
            if (!EXTENSION_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException(path + " contains invalid namespace " + name);
            }
        }
    }
}
