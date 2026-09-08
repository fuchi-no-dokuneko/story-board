package dev.storyblock.api.http;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StrictJsonRequestUniqueStringsFactory {
    static List<String> uniqueStrings(Map<String, Object> value, String field, String path)  {
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
}
