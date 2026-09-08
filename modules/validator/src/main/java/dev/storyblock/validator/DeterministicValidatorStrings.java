package dev.storyblock.validator;

import java.util.Set;
import java.util.TreeSet;

final class DeterministicValidatorStrings {
    static Set<String> strings(Object value) {
        Set<String> strings = new TreeSet<>();
        DeterministicValidatorAddStrings.addStrings(strings, value);
        return Set.copyOf(strings);
    }
}
