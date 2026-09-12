package dev.storyblock.application;

import java.time.Duration;
import java.util.Objects;

final class StyleAnalysisServiceValidateRetention {
    static void validateRetention(Duration retention) {
        Objects.requireNonNull(retention, "retention");
        if (retention.compareTo(Duration.ofHours(1)) < 0
                || retention.compareTo(Duration.ofDays(365)) > 0) {
            throw new IllegalArgumentException(
                    "Style analysis retention must be between one hour and 365 days"
            );
        }
    }
}
