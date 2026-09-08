package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.style.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class StyleAnalysisService {
  public static final Duration DEFAULT_RETENTION = Duration.ofDays(30);
  public static final int DEFAULT_MAX_ATTEMPTS = 3;

  final RevisionStore revisions;
  final StyleProfileStore profiles;
  final StyleAnalysisStore analyses;
  final StyleAnalysisExecutor executor;

  public StyleAnalysisService(
      RevisionStore revisions,
      StyleProfileStore profiles,
      StyleAnalysisStore analyses
  ) {
    this(revisions, profiles, analyses, new StyleAnalysisExecutor());
  }

  StyleAnalysisService(
      RevisionStore revisions,
      StyleProfileStore profiles,
      StyleAnalysisStore analyses,
      StyleAnalysisExecutor executor
  ) {
    this.revisions = Objects.requireNonNull(revisions, "revisions");
    this.profiles = Objects.requireNonNull(profiles, "profiles");
    this.analyses = Objects.requireNonNull(analyses, "analyses");
    this.executor = Objects.requireNonNull(executor, "executor");
  }

  public StyleAnalysisJobSaveResult request(
      Ids.NovelId novelId,
      Ids.RevisionId revisionId,
      String expectedRevisionHash,
      Ids.StyleProfileId profileId,
      Ids.StyleProfileVersionId profileVersionId,
      Ids.BlockId fromBlockId,
      Ids.BlockId toBlockId,
      StyleMaskingLexicon lexicon,
      int maxAttempts,
      Duration retention,
      String idempotencyKey,
      AuditContext auditContext
  ) {
    return StyleAnalysisServiceRequestAction.request(this, novelId, revisionId, expectedRevisionHash, profileId, profileVersionId, fromBlockId, toBlockId, lexicon, maxAttempts, retention, idempotencyKey, auditContext);
  }

  public StyleAnalysisJob getJob(Ids.JobId jobId) {
    return analyses.getStyleAnalysisJob(jobId);
  }

  public StyleAnalysisJob getAnalysis(Ids.StyleAnalysisId analysisId) {
    return analyses.getStyleAnalysis(analysisId);
  }

  public Optional<StyleAnalysisLease> claim(
      Ids.NovelId novelId,
      String leaseOwner,
      Duration leaseDuration,
      String idempotencyKey,
      Instant claimedAt
  ) {
    String requestHash = StyleAnalysisClaimCommand.hash(
        novelId, leaseOwner, leaseDuration
    );
    return analyses.claimStyleAnalysis(new StyleAnalysisClaimCommand(
        novelId,
        leaseOwner,
        leaseDuration,
        idempotencyKey,
        requestHash,
        claimedAt
    ));
  }

  public StyleAnalysisCompletionResult execute(
      StyleAnalysisLease lease,
      String idempotencyKey,
      Instant completedAt
  ) {
    return StyleAnalysisServiceExecuteAction.execute(this, lease, idempotencyKey, completedAt);
  }

  public StyleAnalysisCompletionResult complete(
      StyleAnalysisCompletionCommand command
  ) {
    return analyses.completeStyleAnalysis(command);
  }

  public StyleAnalysisJob fail(
      Ids.JobId jobId,
      String leaseOwner,
      int attempt,
      String expectedStatusHash,
      String failureCode,
      Instant failedAt
  ) {
    return analyses.failStyleAnalysis(
        jobId,
        leaseOwner,
        attempt,
        expectedStatusHash,
        failureCode,
        failedAt
    );
  }

  public Optional<StyleAnalysisResult> result(Ids.StyleAnalysisId analysisId) {
    return analyses.findStyleAnalysisResult(analysisId);
  }

  public StyleAnalysisWindowPage windows(
      Ids.StyleAnalysisId analysisId,
      String cursor,
      int limit
  ) {
    return StyleAnalysisServiceWindowsAction.windows(this, analysisId, cursor, limit);
  }

  public void requireArtifactAvailable(Ids.ArtifactId artifactId, Instant now) {
    analyses.findStyleArtifactExpiry(artifactId).ifPresent(expiry -> {
      if (!now.isBefore(expiry)) {
        throw new ExpiredStyleArtifactException(artifactId);
      }
    });
  }

}
