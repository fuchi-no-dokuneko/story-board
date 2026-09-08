package dev.storyblock.validator;

import java.util.Map;

final class DeterministicValidatorIsMode {
    static boolean isMode(Object value, String expectedMode) {
        return value instanceof Map<?, ?> map && expectedMode.equals(map.get("mode"));
    }
}
