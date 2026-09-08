package dev.storyblock.detector;

import java.util.Objects;

final class AdjacentMetadataDetectorComparableChange {
    static boolean comparableChange(Object before, Object after) {
        return AdjacentMetadataDetectorComparable.comparable(before) && AdjacentMetadataDetectorComparable.comparable(after) && !Objects.equals(before, after);
    }
}
