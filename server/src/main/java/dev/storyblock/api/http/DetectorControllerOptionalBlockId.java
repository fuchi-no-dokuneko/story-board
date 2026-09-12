package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;

final class DetectorControllerOptionalBlockId {
    static Ids.BlockId optionalBlockId(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String id)) {
            throw new IllegalArgumentException(
                    "detector request." + field + " must be a string or null"
            );
        }
        return new Ids.BlockId(id);
    }
}
