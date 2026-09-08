package dev.storyblock.style;

import java.util.Map;

final class StyleAnalysisBlockScalar {
    static String scalar(Object value, String fallback) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        if (value instanceof Map<?, ?> map) {
            for (String field : java.util.List.of("value", "mode", "label")) {
                Object nested = map.get(field);
                if (nested instanceof String text && !text.isBlank()) {
                    return text;
                }
            }
        }
        return fallback;
    }
}
