package dev.storyblock.domain;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

final class StableIdsDeriveFactory {
    static String derive(String prefix, String sourceId, String discriminator) {
        StableIdsRequirePrefix.requirePrefix(prefix);
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(discriminator, "discriminator");
        if (ShortIds.PREFIXES.contains(prefix)) {
            return ShortIds.derive(prefix, sourceId, discriminator);
        }
        ByteBuffer digest = ByteBuffer.wrap(IdentityDigest.of(sourceId + "\0" + discriminator));
        long high = (digest.getLong() & 0xffffffffffff0fffL) | 0x7000L;
        long low = (digest.getLong() & 0x3fffffffffffffffL) | 0x8000000000000000L;
        return prefix + "_" + new UUID(high, low);
    }
}
