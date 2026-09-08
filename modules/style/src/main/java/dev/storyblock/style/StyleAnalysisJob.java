package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.time.Instant;
import java.util.Map;
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
  static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
  static final Pattern OWNER = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");
  static final Pattern FAILURE = Pattern.compile("[a-z][a-z0-9._-]{1,63}");

  public StyleAnalysisJob {
    StyleAnalysisJobValidation.validate(jobId, analysisId, snapshot, status, leaseOwner, leaseUntil, attempt, maxAttempts, idempotencyKey, requestHash, resultArtifactId, resultHash, failureCode, auditContext, retentionUntil, createdAt, updatedAt);
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
    return StyleAnalysisJobQueuedFactory.queued(jobId, analysisId, snapshot, maxAttempts, idempotencyKey, requestHash, auditContext, retentionUntil, createdAt);
  }

  public String statusHash() {
    return CanonicalJson.hash(statusValue());
  }

  public Map<String, Object> statusValue() {
    return StyleAnalysisJobStatusValueAction.statusValue(this);
  }

}
