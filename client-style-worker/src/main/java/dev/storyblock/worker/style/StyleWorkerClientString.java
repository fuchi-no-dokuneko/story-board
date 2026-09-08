package dev.storyblock.worker.style;

import java.util.Map;

final class StyleWorkerClientString {
    static String string(Map<String, Object> value, String field) {
        Object raw = value.get(field);
        if (!(raw instanceof String text)) {
            throw new StyleWorkerProtocolException(
                    "Style job result response." + field + " must be a string"
            );
        }
        return text;
    }
}
