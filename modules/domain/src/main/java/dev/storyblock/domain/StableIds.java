package dev.storyblock.domain;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class StableIds {
    private static final Pattern PREFIX = Pattern.compile("[a-z][a-z0-9]{1,7}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private StableIds() {
    }

    public static String generate(String prefix) {
        return generate(prefix, Clock.systemUTC());
    }

    static String generate(String prefix, Clock clock) {
        requirePrefix(prefix);
        Objects.requireNonNull(clock, "clock");

        long timestamp = clock.millis() & 0x0000FFFFFFFFFFFFL;
        long randomA = RANDOM.nextInt(1 << 12);
        long mostSignificant = (timestamp << 16) | 0x7000L | randomA;
        long leastSignificant = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL)
                | 0x8000000000000000L;
        return prefix + "_" + new UUID(mostSignificant, leastSignificant);
    }

    public static String require(String value, String prefix) {
        Objects.requireNonNull(value, "value");
        requirePrefix(prefix);
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

    private static void requirePrefix(String prefix) {
        if (prefix == null || !PREFIX.matcher(prefix).matches()) {
            throw new IllegalArgumentException("Invalid identifier prefix");
        }
    }
}
