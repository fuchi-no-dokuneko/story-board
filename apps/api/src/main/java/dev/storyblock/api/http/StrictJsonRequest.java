package dev.storyblock.api.http;

import dev.storyblock.contracts.CanonicalJson;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StrictJsonRequest {
    private StrictJsonRequest() {
    }

    static Map<String, Object> parseObject(byte[] value, String path) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = CanonicalJson.mapper().readValue(value, Map.class);
            return object(parsed, path);
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(path + " is malformed", failure);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
        }
        return (Map<String, Object>) map;
    }

    static String string(Map<String, Object> value, String field, String path) {
        Object entry = value.get(field);
        if (!(entry instanceof String text)) {
            throw new IllegalArgumentException(path + "." + field + " must be a string");
        }
        return text;
    }

    static Instant instant(Map<String, Object> value, String field, String path) {
        String text = string(value, field, path);
        try {
            Instant instant = Instant.parse(text);
            if (!instant.toString().equals(text)) {
                throw new IllegalArgumentException(
                        path + "." + field + " must use canonical UTC form"
                );
            }
            return instant;
        } catch (DateTimeParseException failure) {
            throw new IllegalArgumentException(
                    path + "." + field + " must be an ISO-8601 instant", failure
            );
        }
    }

    static List<String> uniqueStrings(
            Map<String, Object> value,
            String field,
            String path
    ) {
        Object entry = value.get(field);
        if (!(entry instanceof List<?> values)) {
            throw new IllegalArgumentException(path + "." + field + " must be an array");
        }
        List<String> result = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (Object item : values) {
            if (!(item instanceof String text) || !unique.add(text)) {
                throw new IllegalArgumentException(
                        path + "." + field + " must contain unique strings"
                );
            }
            result.add(text);
        }
        return List.copyOf(result);
    }

    static void requireKeys(
            Map<String, Object> value,
            Set<String> expected,
            String path
    ) {
        for (String field : expected) {
            if (!value.containsKey(field)) {
                throw new IllegalArgumentException(path + " is missing " + field);
            }
        }
        for (String field : value.keySet()) {
            if (!expected.contains(field)) {
                throw new IllegalArgumentException(
                        path + " contains unknown field " + field
                );
            }
        }
    }

    static String unquoteEtag(String value) {
        if (value == null || value.length() < 2
                || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') {
            throw new IllegalArgumentException("If-Match is not a quoted strong ETag");
        }
        return value.substring(1, value.length() - 1);
    }
}
