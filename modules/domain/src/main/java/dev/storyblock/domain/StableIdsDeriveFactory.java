package dev.storyblock.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

final class StableIdsDeriveFactory {
    static String derive(String prefix, String sourceId, String discriminator)  {
        StableIdsRequirePrefix.requirePrefix(prefix);
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(discriminator, "discriminator");
        int separator = sourceId.indexOf('_');
        if (separator < 2) {
            throw new IllegalArgumentException("Source identifier has no typed UUID prefix");
        }
        UUID source;
        try {
            source = UUID.fromString(sourceId.substring(separator + 1));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Source identifier does not contain a UUID", exception);
        }
        if (source.version() != 7 || source.variant() != 2) {
            throw new IllegalArgumentException("Source identifier must contain an RFC 9562 UUIDv7");
        }

        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(
                    (sourceId + "\0" + discriminator).getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM does not provide SHA-256", exception);
        }
        long randomA = ((digest[0] & 0xffL) << 8 | (digest[1] & 0xffL)) & 0x0fffL;
        long randomB = 0L;
        for (int index = 2; index < 10; index++) {
            randomB = (randomB << 8) | (digest[index] & 0xffL);
        }
        long timestampBits = source.getMostSignificantBits() & 0xffffffffffff0000L;
        long mostSignificant = timestampBits | 0x7000L | randomA;
        long leastSignificant = (randomB & 0x3fffffffffffffffL) | 0x8000000000000000L;
        return prefix + "_" + new UUID(mostSignificant, leastSignificant);
    }
}
