package dev.storyblock.style;

import java.util.Map;

final class StyleFeatureAnalyzerNestedString {
    static String nestedString(Object value, String field) {
        if (value instanceof Map<?, ?> map && map.get(field) instanceof String text
                && !text.isBlank()) {
            return text;
        }
        return null;
    }
}
