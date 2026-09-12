package dev.storyblock.application;

import dev.storyblock.rewrite.policy.RewritePolicyModule;
import java.time.Duration;
import java.util.Objects;

final class RewriteGateServiceValidateCooldown {
    static void validateCooldown(Duration cooldown) {
        Objects.requireNonNull(cooldown, "cooldown");
        if (cooldown.compareTo(RewritePolicyModule.MIN_COOLDOWN) < 0
                || cooldown.compareTo(RewritePolicyModule.MAX_COOLDOWN) > 0) {
            throw new IllegalArgumentException("Rewrite cooldown duration is invalid");
        }
    }
}
