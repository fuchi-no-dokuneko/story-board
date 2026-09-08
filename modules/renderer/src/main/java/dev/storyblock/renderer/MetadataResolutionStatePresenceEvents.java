package dev.storyblock.renderer;

import dev.storyblock.domain.CanonicalValues;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MetadataResolutionStatePresenceEvents {
    static List<Map<String, Object>> presenceEvents(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> entries)) {
            throw new IllegalArgumentException("block.meta.presence_events must be a list");
        }

        List<Map<String, Object>> events = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            Object entry = entries.get(index);
            if (!(entry instanceof Map<?, ?> event)) {
                throw new IllegalArgumentException(
                        "block.meta.presence_events[" + index + "] must be an object"
                );
            }
            Map<String, Object> typed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> field : event.entrySet()) {
                if (!(field.getKey() instanceof String key)) {
                    throw new IllegalArgumentException(
                            "block.meta.presence_events[" + index + "] contains a non-string key"
                    );
                }
                typed.put(key, field.getValue());
            }
            events.add(CanonicalValues.freezeMap(
                    typed, "block.meta.presence_events[" + index + "]"
            ));
        }
        return List.copyOf(events);
    }
}
