package dev.storyblock.validator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DeterministicValidatorMaps {
    static List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof Map<?, ?> raw) {
                Map<String, Object> typed = new LinkedHashMap<>();
                for (Map.Entry<?, ?> field : raw.entrySet()) {
                    if (field.getKey() instanceof String key) {
                        typed.put(key, field.getValue());
                    }
                }
                maps.add(java.util.Collections.unmodifiableMap(typed));
            }
        }
        return List.copyOf(maps);
    }
}
