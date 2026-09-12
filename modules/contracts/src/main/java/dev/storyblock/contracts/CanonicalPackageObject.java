package dev.storyblock.contracts;

import java.util.Map;

final class CanonicalPackageObject {
    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new CanonicalPackageException(path + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new CanonicalPackageException(path + " contains a non-string key");
            }
        }
        return (Map<String, Object>) map;
    }
}
