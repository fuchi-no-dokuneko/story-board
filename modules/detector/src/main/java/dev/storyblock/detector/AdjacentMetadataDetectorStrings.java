package dev.storyblock.detector;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

final class AdjacentMetadataDetectorStrings {
    static Set<String> strings(Object raw) {
        Set<String> values = new TreeSet<>();
        if (raw instanceof List<?> entries) {
            for (Object entry : entries) {
                if (entry instanceof String value) {
                    values.add(value);
                }
            }
        }
        return values;
    }
}
