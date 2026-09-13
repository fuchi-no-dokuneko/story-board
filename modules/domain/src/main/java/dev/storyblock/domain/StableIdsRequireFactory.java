package dev.storyblock.domain;

import java.util.Objects;
import java.util.UUID;

final class StableIdsRequireFactory {
    static String require(String value, String prefix)  {
        Objects.requireNonNull(value, "value");
        StableIdsRequirePrefix.requirePrefix(prefix);
        if (ShortIds.PREFIXES.contains(prefix)) return ShortIds.require(value, prefix);
        String expectedPrefix = prefix + "_";
        if (!value.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("Expected " + prefix + " identifier");
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(value.substring(expectedPrefix.length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Identifier does not contain a UUID", exception);
        }
        if (uuid.version() != 7 || uuid.variant() != 2) {
            throw new IllegalArgumentException("Identifier must contain an RFC 9562 UUIDv7");
        }
        return value;
    }
}
