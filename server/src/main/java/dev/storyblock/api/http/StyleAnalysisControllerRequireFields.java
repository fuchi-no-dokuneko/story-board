package dev.storyblock.api.http;

import java.util.Map;
import java.util.Set;

final class StyleAnalysisControllerRequireFields {
    static void requireFields(
            Map<String, Object> request,
            Set<String> required,
            Set<String> allowed,
            String path
    ) {
        for (String field : required) {
            if (!request.containsKey(field)) {
                throw new IllegalArgumentException(path + " is missing " + field);
            }
        }
        for (String field : request.keySet()) {
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException(path + " contains unknown field " + field);
            }
        }
    }
}
