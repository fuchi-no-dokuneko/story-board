package dev.storyblock.style;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StyleCanonical {
    StyleCanonical() {
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

    static String optionalString(Map<String, Object> value, String field, String path) {
        Object raw = value.get(field);
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String text)) {
            throw new IllegalArgumentException(
                    path + "." + field + " must be a string or null"
            );
        }
        return text;
    }

    static int integer(Map<String, Object> value, String field, String path) {
        return StyleCanonicalIntegerFactory.integer(value, field, path);
    }

    static BigDecimal decimal(Map<String, Object> value, String field, String path) {
        return StyleCanonicalDecimalFactory.decimal(value, field, path);
    }

    static boolean bool(Map<String, Object> value, String field, String path) {
        Object raw = value.get(field);
        if (!(raw instanceof Boolean result)) {
            throw new IllegalArgumentException(path + "." + field + " must be boolean");
        }
        return result;
    }

    static Instant instant(Map<String, Object> value, String field, String path) {
        return StyleCanonicalInstantFactory.instant(value, field, path);
    }

    static Map<String, Object> object(Object value, String path) {
        return StyleCanonicalObjectFactory.object(value, path);
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
        return StyleCanonicalStringsFactory.strings(value, path);
    }

    static Map<String, BigDecimal> decimals(Object value, String path) {
        return StyleCanonicalDecimalsFactory.decimals(value, path);
    }

    static List<BigDecimal> decimalList(Object value, String path) {
        return StyleCanonicalDecimalListFactory.decimalList(value, path);
    }
}
