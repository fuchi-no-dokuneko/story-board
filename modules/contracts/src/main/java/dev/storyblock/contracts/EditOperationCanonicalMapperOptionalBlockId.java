package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;

final class EditOperationCanonicalMapperOptionalBlockId {
    static Ids.BlockId optionalBlockId(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException("Optional block ID must be a string or null");
        }
        return new Ids.BlockId(string);
    }
}
