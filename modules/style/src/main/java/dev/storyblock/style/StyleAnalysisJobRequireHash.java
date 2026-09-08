package dev.storyblock.style;

import static dev.storyblock.style.StyleAnalysisJob.HASH;

final class StyleAnalysisJobRequireHash {
    static void requireHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Style analysis " + field + " hash is invalid"
            );
        }
    }
}
