package dev.storyblock.api.http;

import java.util.Map;
import java.util.Set;

final class StrictJsonRequestRequireKeysFactory {
    static void requireKeys(Map<String, Object> value, Set<String> expected, String path)  {
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
}
