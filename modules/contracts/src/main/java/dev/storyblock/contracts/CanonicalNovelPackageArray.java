package dev.storyblock.contracts;

import java.util.ArrayList;
import java.util.List;

final class CanonicalNovelPackageArray {
    static List<Object> array(Object value, String path) {
        if (!(value instanceof List<?> list)) {
            throw new CanonicalPackageException(path + " must be an array");
        }
        return new ArrayList<>(list);
    }
}
