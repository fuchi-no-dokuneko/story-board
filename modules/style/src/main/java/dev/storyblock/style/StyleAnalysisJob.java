package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record StyleAnalysisJob(
        Ids.JobId jobId,
        Ids.StyleAnalysisId analysisId,
        StyleAnalysisSnapshot snapshot,
        StyleAnalysisJobStatus status,
        String leaseOwner,
        Instant leaseUntil,
        int attempt,
        int maxAttempts,
        String idempotencyKey,
        String requestHash,
        Ids.ArtifactId resultArtifactId,
        String resultHash,
        String failureCode,
        AuditContext auditContext,
        Instant retentionUntil,
        Instant createdAt,
        Instant updatedAt
) {
    public static final int MIN_ATTEMPTS = 1;
    public static final int MAX_ATTEMPTS = 20;
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Pattern OWNER = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");
    private static final Pattern FAILURE = Pattern.compile("[a-z][a-z0-9._-]{1,63}");

    public StyleAnalysisJob {
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(status, "status");
        if (attempt < 0 || maxAttempts < MIN_ATTEMPTS || maxAttempts > MAX_ATTEMPTS
                || attempt > maxAttempts) {
            throw new IllegalArgumentException("Style analysis attempt bounds are invalid");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException("Style analysis idempotency key is invalid");
        }
        requireHash(requestHash, "request");
        Objects.requireNonNull(auditContext, "auditContext");
        Objects.requireNonNull(retentionUntil, "retentionUntil");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (!auditContext.occurredAt().equals(createdAt)
                || !retentionUntil.isAfter(createdAt) || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Style analysis timestamps are invalid");
        }
        validateState(
                status,
                leaseOwner,
                leaseUntil,
                attempt,
                resultArtifactId,
                resultHash,
                failureCode,
                updatedAt
        );
    }

    public static StyleAnalysisJob queued(
            Ids.JobId jobId,
            Ids.StyleAnalysisId analysisId,
            StyleAnalysisSnapshot snapshot,
            int maxAttempts,
            String idempotencyKey,
            String requestHash,
            AuditContext auditContext,
            Instant retentionUntil,
            Instant createdAt
    ) {
        return new StyleAnalysisJob(
                jobId,
                analysisId,
                snapshot,
                StyleAnalysisJobStatus.QUEUED,
                null,
                null,
                0,
                maxAttempts,
                idempotencyKey,
                requestHash,
                null,
                null,
                null,
                auditContext,
                retentionUntil,
                createdAt,
                createdAt
        );
    }

    public String statusHash() {
        return CanonicalJson.hash(statusValue());
    }

    public Map<String, Object> statusValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", analysisId.value());
        value.put("attempt", attempt);
        value.put("created_at", createdAt.toString());
        value.put("created_by", auditContext.actorId());
        value.put("failure_code", failureCode);
        value.put("job_id", jobId.value());
        value.put("lease_owner", leaseOwner);
        value.put("lease_until", leaseUntil == null ? null : leaseUntil.toString());
        value.put("max_attempts", maxAttempts);
        value.put("novel_id", snapshot.novelId().value());
        value.put("profile_version_hash", snapshot.profileVersionHash());
        value.put("result_artifact_id", resultArtifactId == null
                ? null : resultArtifactId.value());
        value.put("result_hash", resultHash);
        value.put("retention_until", retentionUntil.toString());
        value.put("revision_hash", snapshot.revisionHash());
        value.put("revision_id", snapshot.revisionId().value());
        value.put("snapshot_hash", snapshot.snapshotHash());
        value.put("status", status.canonicalName());
        value.put("updated_at", updatedAt.toString());
        return CanonicalValues.freezeMap(value, "style_analysis_job");
    }

    private static void validateState(
            StyleAnalysisJobStatus status,
            String leaseOwner,
            Instant leaseUntil,
            int attempt,
            Ids.ArtifactId resultArtifactId,
            String resultHash,
            String failureCode,
            Instant updatedAt
    ) {
        boolean leased = leaseOwner != null || leaseUntil != null;
        boolean completed = resultArtifactId != null || resultHash != null;
        if (status == StyleAnalysisJobStatus.RUNNING) {
            if (leaseOwner == null || !OWNER.matcher(leaseOwner).matches()
                    || leaseUntil == null || !leaseUntil.isAfter(updatedAt)
                    || attempt < 1 || completed || failureCode != null) {
                throw new IllegalArgumentException("Running style analysis state is invalid");
            }
            return;
        }
        if (leased) {
            throw new IllegalArgumentException("Non-running style analysis cannot hold a lease");
        }
        if (status == StyleAnalysisJobStatus.QUEUED) {
            if (attempt != 0 || completed || failureCode != null) {
                throw new IllegalArgumentException("Queued style analysis state is invalid");
            }
        } else if (status == StyleAnalysisJobStatus.SUCCEEDED) {
            if (attempt < 1 || resultArtifactId == null) {
                throw new IllegalArgumentException("Succeeded style analysis lacks a result");
            }
            requireHash(resultHash, "result");
            if (failureCode != null) {
                throw new IllegalArgumentException("Succeeded style analysis has a failure");
            }
        } else if (status == StyleAnalysisJobStatus.FAILED
                && (attempt < 1 || failureCode == null
                || !FAILURE.matcher(failureCode).matches() || completed)) {
            throw new IllegalArgumentException("Failed style analysis state is invalid");
        }
    }

    private static void requireHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Style analysis " + field + " hash is invalid"
            );
        }
    }
}
