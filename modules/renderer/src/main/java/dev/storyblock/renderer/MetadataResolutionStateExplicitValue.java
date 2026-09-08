package dev.storyblock.renderer;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;
import static dev.storyblock.renderer.MetadataResolutionState.EXPLICIT_CONTROL_FIELDS;

final class MetadataResolutionStateExplicitValue {
    static Object explicitValue(Map<?, ?> observation, String path) {
        if (observation.containsKey("value")) {
            Object value = observation.get("value");
            if (value == null) {
                throw new IllegalArgumentException(path + " explicit mode requires a value");
            }
            return CanonicalValues.freeze(value, path + ".value");
        }

        Map<String, Object> inlineValue = new LinkedHashMap<>();
        for (Map.Entry<?, ?> field : observation.entrySet()) {
            if (!(field.getKey() instanceof String key)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
            if (!EXPLICIT_CONTROL_FIELDS.contains(key)) {
                inlineValue.put(key, field.getValue());
            }
        }
        if (inlineValue.isEmpty()) {
            throw new IllegalArgumentException(path + " explicit mode requires a value");
        }
        return CanonicalValues.freezeMap(inlineValue, path + ".value");
    }
}
