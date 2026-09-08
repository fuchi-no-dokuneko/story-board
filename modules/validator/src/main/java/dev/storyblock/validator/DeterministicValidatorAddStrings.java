package dev.storyblock.validator;

import java.util.Collection;
import java.util.Set;

final class DeterministicValidatorAddStrings {
    static void addStrings(Set<String> destination, Object value) {
        if (value instanceof String string && !string.isBlank()) {
            destination.add(string);
        } else if (value instanceof Collection<?> values) {
            for (Object entry : values) {
                if (entry instanceof String string && !string.isBlank()) {
                    destination.add(string);
                }
            }
        }
    }
}
