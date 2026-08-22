package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record ReserveRewriteCandidateCommand(
        RewriteCandidateReservation reservation,
        String idempotencyKey,
        String requestHash
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public ReserveRewriteCandidateCommand {
        Objects.requireNonNull(reservation, "reservation");
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException(
                    "Rewrite reservation idempotency key is invalid"
            );
        }
        if (requestHash == null || !HASH.matcher(requestHash).matches()) {
            throw new IllegalArgumentException(
                    "Rewrite reservation request hash is invalid"
            );
        }
    }

    public static String hash(RewriteEligibility eligibility, Duration cooldown) {
        Objects.requireNonNull(eligibility, "eligibility");
        Objects.requireNonNull(cooldown, "cooldown");
        return CanonicalJson.hash(Map.of(
                "cooldown_seconds", cooldown.toSeconds(),
                "eligibility_hash", eligibility.eligibilityHash(),
                "policy_version", RewritePolicyModule.VERSION
        ));
    }
}
