package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record StyleAnalysisClaimCommand(
        Ids.NovelId novelId,
        String leaseOwner,
        Duration leaseDuration,
        String idempotencyKey,
        String requestHash,
        Instant claimedAt
) {
    private static final Pattern OWNER = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");

    public StyleAnalysisClaimCommand {
        Objects.requireNonNull(novelId, "novelId");
        if (leaseOwner == null || !OWNER.matcher(leaseOwner).matches()) {
            throw new IllegalArgumentException("Style analysis lease owner is invalid");
        }
        Objects.requireNonNull(leaseDuration, "leaseDuration");
        if (leaseDuration.compareTo(Duration.ofSeconds(15)) < 0
                || leaseDuration.compareTo(Duration.ofMinutes(30)) > 0) {
            throw new IllegalArgumentException(
                    "Style analysis lease must be between 15 seconds and 30 minutes"
            );
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException("Style claim idempotency key is invalid");
        }
        String calculated = hash(novelId, leaseOwner, leaseDuration);
        if (!calculated.equals(requestHash)) {
            throw new IllegalArgumentException("Style claim request hash is invalid");
        }
        Objects.requireNonNull(claimedAt, "claimedAt");
    }

    public static String hash(
            Ids.NovelId novelId,
            String leaseOwner,
            Duration leaseDuration
    ) {
        return CanonicalJson.hash(Map.of(
                "lease_owner", leaseOwner,
                "lease_seconds", leaseDuration.toSeconds(),
                "novel_id", novelId.value()
        ));
    }
}
