package dev.storyblock.detector;

import java.util.HashSet;
import java.util.List;

final class DetectorFindingDistinctCopy {
    static <T> List<T> distinctCopy(List<T> values, String label) {
        List<T> copy = List.copyOf(values);
        if (new HashSet<>(copy).size() != copy.size()) {
            throw new IllegalArgumentException(label + " cannot contain duplicates");
        }
        return copy;
    }
}
