package dev.storyblock.contracts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class CanonicalRevisionRequireList {
    static List<Object> requireList(Map<String, Object> object, String field, String path) {
        Object value = object.get(field);
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(path + "." + field + " must be an array");
        }
        return new ArrayList<>(list);
    }
}
