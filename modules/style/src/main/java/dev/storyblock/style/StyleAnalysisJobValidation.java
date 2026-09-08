package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.time.Instant;
import java.util.Objects;
import static dev.storyblock.style.StyleAnalysisJob.*;

final class StyleAnalysisJobValidation {
  static void validate(Ids.JobId jobId, Ids.StyleAnalysisId analysisId, StyleAnalysisSnapshot snapshot, StyleAnalysisJobStatus status, String leaseOwner, Instant leaseUntil, int attempt, int maxAttempts, String idempotencyKey, String requestHash, Ids.ArtifactId resultArtifactId, String resultHash, String failureCode, AuditContext auditContext, Instant retentionUntil, Instant createdAt, Instant updatedAt) {
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
        StyleAnalysisJobRequireHash.requireHash(requestHash, "request");
        Objects.requireNonNull(auditContext, "auditContext");
        Objects.requireNonNull(retentionUntil, "retentionUntil");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (!auditContext.occurredAt().equals(createdAt)
            || !retentionUntil.isAfter(createdAt) || updatedAt.isBefore(createdAt)) {
          throw new IllegalArgumentException("Style analysis timestamps are invalid");
        }
        StyleAnalysisJobValidateState.validateState(
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
}
