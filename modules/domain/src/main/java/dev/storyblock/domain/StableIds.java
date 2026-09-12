package dev.storyblock.domain;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.regex.Pattern;

public final class StableIds {
    static final Pattern PREFIX = Pattern.compile("[a-z][a-z0-9]{1,7}");
    static final SecureRandom RANDOM = new SecureRandom();

    StableIds() {
    }

    public static String generate(String prefix) {
        return generate(prefix, Clock.systemUTC());
    }

    public static String derive(String prefix, String sourceId, String discriminator) {
        return StableIdsDeriveFactory.derive(prefix, sourceId, discriminator);
    }

    static String generate(String prefix, Clock clock) {
        return StableIdsGenerateFactory.generate(prefix, clock);
    }

    public static String require(String value, String prefix) {
        return StableIdsRequireFactory.require(value, prefix);
    }

}
