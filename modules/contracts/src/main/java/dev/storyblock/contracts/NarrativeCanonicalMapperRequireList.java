package dev.storyblock.contracts;

import java.util.ArrayList;
import java.util.List;

final class NarrativeCanonicalMapperRequireList {
    static List<Object> requireList(Object value, String path) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        return new ArrayList<>(list);
    }
}
