package dev.storyblock.style;

import java.util.Map;

final class StyleFeatureAnalyzerScalar {
    static String scalar(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        if (value instanceof Map<?, ?> map) {
            Object nested = map.get("value");
            if (nested instanceof String text && !text.isBlank()) {
                return text;
            }
            Object mode = map.get("mode");
            if (mode instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return "unknown";
    }
}
