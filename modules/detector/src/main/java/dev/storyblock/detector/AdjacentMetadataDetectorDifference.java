package dev.storyblock.detector;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

final class AdjacentMetadataDetectorDifference {
    static List<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new TreeSet<>(left);
        result.removeAll(right);
        return List.copyOf(result);
    }
}
