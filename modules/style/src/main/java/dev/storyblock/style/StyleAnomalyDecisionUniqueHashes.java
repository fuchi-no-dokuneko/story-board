package dev.storyblock.style;

import java.util.LinkedHashSet;
import java.util.List;

final class StyleAnomalyDecisionUniqueHashes {
    static List<String> uniqueHashes(List<String> values, String field) {
        values = List.copyOf(values);
        if (new LinkedHashSet<>(values).size() != values.size()
                || values.stream().anyMatch(value ->
                        value == null || !value.matches("sha256:[0-9a-f]{64}")
                )) {
            throw new IllegalArgumentException(
                    "Style decision " + field + " window IDs are invalid"
            );
        }
        return values;
    }
}
