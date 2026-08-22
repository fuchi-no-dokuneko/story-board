package dev.storyblock.rewrite;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RewriteCanonical {
    private RewriteCanonical() {
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
        Object raw = value.get(field);
        if (!(raw instanceof Number number)) {
            throw new IllegalArgumentException(path + "." + field + " must be an integer");
        }
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException(
                    path + "." + field + " must be an exact integer", failure
            );
        }
    }

    static boolean bool(Map<String, Object> value, String field, String path) {
        Object raw = value.get(field);
        if (!(raw instanceof Boolean result)) {
            throw new IllegalArgumentException(path + "." + field + " must be boolean");
        }
        return result;
    }

    static Instant instant(Map<String, Object> value, String field, String path) {
        String raw = string(value, field, path);
        try {
            Instant result = Instant.parse(raw);
            if (!result.toString().equals(raw)) {
                throw new IllegalArgumentException(path + "." + field + " is not canonical");
            }
            return result;
        } catch (DateTimeParseException failure) {
            throw new IllegalArgumentException(path + "." + field + " is invalid", failure);
        }
    }

    static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
            result.put(key, entry.getValue());
        }
        return result;
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
        if (!(value instanceof List<?> raw)) {
            throw new IllegalArgumentException(path + " must be an array");
        }
        List<String> result = new ArrayList<>();
        for (int index = 0; index < raw.size(); index++) {
            if (!(raw.get(index) instanceof String text)) {
                throw new IllegalArgumentException(path + "[" + index + "] must be a string");
            }
            result.add(text);
        }
        return List.copyOf(result);
    }
}
