package dev.storyblock.style;

import static dev.storyblock.style.StyleAnalysisCompletionCommand.HASH;

final class StyleAnalysisCompletionCommandRequireHash {
    static void requireHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Style completion " + field + " hash is invalid"
            );
        }
    }
}
