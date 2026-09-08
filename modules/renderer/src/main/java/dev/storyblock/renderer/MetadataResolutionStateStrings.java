package dev.storyblock.renderer;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

final class MetadataResolutionStateStrings {
    static List<String> strings(Object raw, String path) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> entries)) {
            throw new IllegalArgumentException(path + " must be a list");
        }
        Set<String> sorted = new TreeSet<>();
        for (Object entry : entries) {
            sorted.add(MetadataResolutionStateRequiredString.requiredString(entry, path + " entry"));
        }
        return List.copyOf(sorted);
    }
}
