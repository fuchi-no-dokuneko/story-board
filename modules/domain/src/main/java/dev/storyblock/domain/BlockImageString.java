package dev.storyblock.domain;

import java.util.Map;
import static dev.storyblock.domain.BlockImage.EXTENSION_KEY;

final class BlockImageString {
    static String string(Map<?, ?> map, String field) {
        Object value = map.get(field);
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(EXTENSION_KEY + "." + field + " must be a string");
        }
        return text;
    }
}
