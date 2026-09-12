package dev.storyblock.contracts;

import java.util.Map;

final class CanonicalNovelPackageString {
    static String string(Map<String, Object> value, String field, String path) {
        Object entry = value.get(field);
        if (!(entry instanceof String text)) {
            throw new CanonicalPackageException(path + "." + field + " must be a string");
        }
        return text;
    }
}
