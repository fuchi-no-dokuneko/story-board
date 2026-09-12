package dev.storyblock.style;

import static dev.storyblock.style.StyleAnalysisSnapshot.HASH;

final class StyleAnalysisSnapshotRequireHash {
    static void requireHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Style analysis " + field + " hash is invalid"
            );
        }
    }
}
