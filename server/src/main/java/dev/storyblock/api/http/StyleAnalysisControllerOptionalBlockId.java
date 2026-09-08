package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;

final class StyleAnalysisControllerOptionalBlockId {
    static Ids.BlockId optionalBlockId(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException(
                    "style analysis request." + field + " must be a string or null"
            );
        }
        return new Ids.BlockId(text);
    }
}
