package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.LinkedHashMap;
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
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
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
        StyleCanonical.requireKeys(value, FIELDS, "style_analysis_lease");
        return new StyleAnalysisLease(
                new Ids.JobId(StyleCanonical.string(
                        value, "job_id", "style_analysis_lease"
                )),
                new Ids.StyleAnalysisId(StyleCanonical.string(
                        value, "analysis_id", "style_analysis_lease"
                )),
                StyleAnalysisSnapshot.fromCanonical(StyleCanonical.object(
                        value.get("snapshot"), "style_analysis_lease.snapshot"
                )),
                StyleCanonical.string(value, "lease_owner", "style_analysis_lease"),
                StyleCanonical.integer(value, "attempt", "style_analysis_lease"),
                StyleCanonical.instant(value, "lease_until", "style_analysis_lease"),
                StyleCanonical.instant(
                        value, "retention_until", "style_analysis_lease"
                ),
                StyleCanonical.string(
                        value, "status_hash", "style_analysis_lease"
                ),
                StyleCanonical.bool(
                        value, "idempotent_replay", "style_analysis_lease"
                )
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", analysisId.value());
        value.put("attempt", attempt);
        value.put("idempotent_replay", idempotentReplay);
        value.put("job_id", jobId.value());
        value.put("lease_owner", leaseOwner);
        value.put("lease_until", leaseUntil.toString());
        value.put("retention_until", retentionUntil.toString());
        value.put("snapshot", snapshot.canonicalValue());
        value.put("status_hash", claimedStatusHash);
        return CanonicalValues.freezeMap(value, "style_analysis_lease");
    }
}
