package dev.storyblock.rewrite.policy;

import java.time.Duration;

public final class RewritePolicyModule {
    public static final String VERSION = "rewrite-policy-1.0.0";
    public static final String RESERVATION_SCHEMA_VERSION =
            "rewrite-reservation-1.0.0";
    public static final String FACT_CONTRACT_VERSION = "protected-facts-1.0.0";
    public static final String NEAR_COPY_VERSION = "long-ngram-1.0.0";
    public static final Duration DEFAULT_COOLDOWN = Duration.ofHours(24);
    public static final Duration MIN_COOLDOWN = Duration.ofMinutes(5);
    public static final Duration MAX_COOLDOWN = Duration.ofDays(30);

    private RewritePolicyModule() {
    }
}
