package dev.storyblock.style;

import java.util.ArrayList;
import java.util.List;

final class StyleMaskingLexiconStrings {
    static List<String> strings(Object value, String path) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof String text)) {
                throw new IllegalArgumentException(path + " must contain strings");
            }
            result.add(text);
        }
        return List.copyOf(result);
    }
}
