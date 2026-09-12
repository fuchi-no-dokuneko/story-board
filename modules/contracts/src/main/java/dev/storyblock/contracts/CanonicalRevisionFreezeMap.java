package dev.storyblock.contracts;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

final class CanonicalRevisionFreezeMap {
    static Map<String, Object> freezeMap(Map<?, ?> input, String path) {
        Objects.requireNonNull(input, path);
        Map<String, Object> frozen = new TreeMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
            frozen.put(key, CanonicalRevisionFreezeValue.freezeValue(entry.getValue(), path + "." + key));
        }
        return Collections.unmodifiableMap(frozen);
    }
}
