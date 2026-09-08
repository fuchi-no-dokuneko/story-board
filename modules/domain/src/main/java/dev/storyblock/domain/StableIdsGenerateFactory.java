package dev.storyblock.domain;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

final class StableIdsGenerateFactory {
    static String generate(String prefix, Clock clock)  {
        StableIdsRequirePrefix.requirePrefix(prefix);
        Objects.requireNonNull(clock, "clock");

        long timestamp = clock.millis() & 0x0000FFFFFFFFFFFFL;
        long randomA = StableIds.RANDOM.nextInt(1 << 12);
        long mostSignificant = (timestamp << 16) | 0x7000L | randomA;
        long leastSignificant = (StableIds.RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL)
                | 0x8000000000000000L;
        return prefix + "_" + new UUID(mostSignificant, leastSignificant);
    }
}
