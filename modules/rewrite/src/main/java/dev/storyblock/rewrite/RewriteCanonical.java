package dev.storyblock.rewrite;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RewriteCanonical {
    RewriteCanonical() {
    }

    static void requireKeys(Map<String, Object> value, Set<String> fields, String path) {
        if (!value.keySet().equals(fields)) {
            throw new IllegalArgumentException(path + " fields are invalid");
        }
    }

    static String string(Map<String, Object> value, String field, String path) {
        Object raw = value.get(field);
        if (!(raw instanceof String text)) {
            throw new IllegalArgumentException(path + "." + field + " must be a string");
        }
        return text;
    }

    static int integer(Map<String, Object> value, String field, String path) {
        return RewriteCanonicalIntegerFactory.integer(value, field, path);
    }

    static boolean bool(Map<String, Object> value, String field, String path) {
        Object raw = value.get(field);
        if (!(raw instanceof Boolean result)) {
            throw new IllegalArgumentException(path + "." + field + " must be boolean");
        }
        return result;
    }

    static Instant instant(Map<String, Object> value, String field, String path) {
        return RewriteCanonicalInstantFactory.instant(value, field, path);
    }

    static Map<String, Object> object(Object value, String path) {
        return RewriteCanonicalObjectFactory.object(value, path);
    }

    static List<Map<String, Object>> objects(Object value, String path) {
        if (!(value instanceof List<?> raw)) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < raw.size(); index++) {
            result.add(object(raw.get(index), path + "[" + index + "]"));
        }
        return List.copyOf(result);
    }

    static List<String> strings(Object value, String path) {
        return RewriteCanonicalStringsFactory.strings(value, path);
    }
}
