package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;

final class RenderControllerOptionalBlockId {
    static Ids.BlockId optionalBlockId(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String id)) {
            throw new IllegalArgumentException(
                    "render request." + field + " must be a string or null"
            );
        }
        return new Ids.BlockId(id);
    }
}
