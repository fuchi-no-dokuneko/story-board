package dev.storyblock.contracts;

import java.util.List;

final class EditOperationCanonicalMapperArray {
    static List<Object> array(Object value, String path) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        return new java.util.ArrayList<>(list);
    }
}
