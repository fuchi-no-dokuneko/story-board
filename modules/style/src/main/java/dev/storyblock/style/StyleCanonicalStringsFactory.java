package dev.storyblock.style;

import java.util.ArrayList;
import java.util.List;

final class StyleCanonicalStringsFactory {
    static List<String> strings(Object value, String path)  {
        if (!(value instanceof List<?> raw)) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        List<String> result = new ArrayList<>();
        for (int index = 0; index < raw.size(); index++) {
            if (!(raw.get(index) instanceof String text)) {
                throw new IllegalArgumentException(
                        path + "[" + index + "] must be a string"
                );
            }
            result.add(text);
        }
        return List.copyOf(result);
    }
}
