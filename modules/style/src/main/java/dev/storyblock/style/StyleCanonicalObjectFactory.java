package dev.storyblock.style;

import java.util.LinkedHashMap;
import java.util.Map;

final class StyleCanonicalObjectFactory {
    static Map<String, Object> object(Object value, String path)  {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }
}
