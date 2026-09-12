package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.style.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class StyleAnalysisService extends StyleAnalysisQueries {
  public static final Duration DEFAULT_RETENTION = Duration.ofDays(30);
  public static final int DEFAULT_MAX_ATTEMPTS = 3;

  final RevisionStore revisions;
  final StyleProfileStore profiles;
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
    super(analyses);
    this.revisions = Objects.requireNonNull(revisions, "revisions");
    this.profiles = Objects.requireNonNull(profiles, "profiles");
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

  public StyleAnalysisWindowPage windows(
      Ids.StyleAnalysisId analysisId,
      String cursor,
      int limit
  ) {
    return StyleAnalysisServiceWindowsAction.windows(this, analysisId, cursor, limit);
  }


}
