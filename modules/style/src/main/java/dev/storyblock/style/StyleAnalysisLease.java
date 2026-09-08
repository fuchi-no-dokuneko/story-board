package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleAnalysisLease(
        Ids.JobId jobId,
        Ids.StyleAnalysisId analysisId,
        StyleAnalysisSnapshot snapshot,
        String leaseOwner,
        int attempt,
        Instant leaseUntil,
        Instant retentionUntil,
        String claimedStatusHash,
        boolean idempotentReplay
) {
    static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    static final Set<String> FIELDS = Set.of(
            "analysis_id", "attempt", "idempotent_replay", "job_id",
            "lease_owner", "lease_until", "retention_until", "snapshot",
            "status_hash"
    );

    public StyleAnalysisLease {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(snapshot, "snapshot");
        if (leaseOwner == null || leaseOwner.isBlank() || attempt < 1) {
            throw new IllegalArgumentException("Style analysis lease identity is invalid");
        }
        Objects.requireNonNull(leaseUntil, "leaseUntil");
        Objects.requireNonNull(retentionUntil, "retentionUntil");
        if (!retentionUntil.isAfter(snapshot.profileVersion().createdAt())) {
            throw new IllegalArgumentException("Style analysis retention is invalid");
        }
        if (claimedStatusHash == null || !HASH.matcher(claimedStatusHash).matches()) {
            throw new IllegalArgumentException("Style analysis lease status hash is invalid");
        }
    }

    public static StyleAnalysisLease fromCanonical(Map<String, Object> value) {
        return StyleAnalysisLeaseFromCanonicalFactory.fromCanonical(value);
    }

    public Map<String, Object> canonicalValue() {
        return StyleAnalysisLeaseCanonicalValueAction.canonicalValue(this);
    }
}
