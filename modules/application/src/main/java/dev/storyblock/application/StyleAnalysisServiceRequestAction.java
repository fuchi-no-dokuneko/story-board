package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.StoredRevision;
import dev.storyblock.style.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class StyleAnalysisServiceRequestAction {
  static StyleAnalysisJobSaveResult request(StyleAnalysisService self, Ids.NovelId novelId, Ids.RevisionId revisionId, String expectedRevisionHash, Ids.StyleProfileId profileId, Ids.StyleProfileVersionId profileVersionId, Ids.BlockId fromBlockId, Ids.BlockId toBlockId, StyleMaskingLexicon lexicon, int maxAttempts, Duration retention, String idempotencyKey, AuditContext auditContext)  {
    Objects.requireNonNull(novelId, "novelId");
    Objects.requireNonNull(revisionId, "revisionId");
    Objects.requireNonNull(profileId, "profileId");
    Objects.requireNonNull(profileVersionId, "profileVersionId");
    Objects.requireNonNull(lexicon, "lexicon");
    Objects.requireNonNull(auditContext, "auditContext");
    StyleAnalysisServiceValidateRetention.validateRetention(retention);

    StoredRevision stored = self.revisions.getRevision(novelId, revisionId);
    if (!stored.contentHash().equals(expectedRevisionHash)) {
      throw new StyleAnalysisSnapshotConflictException(stored.contentHash());
    }
    StyleProfileVersionView profile = self.profiles.getStyleProfileVersion(
        profileId, profileVersionId
    );
    if (!profile.profileVersion().content().scope().novelId().equals(novelId)) {
      throw new StyleLifecycleConflictException(
          "Style analysis profile belongs to a different novel"
      );
    }
    if (profile.profileVersion().content().calibrationProfile().isEmpty()) {
      throw new StyleLifecycleConflictException(
          "Style analysis requires an immutable calibrated profile version"
      );
    }
    List<StyleAnalysisBlock> selected = StyleAnalysisServiceSelectBlocks.selectBlocks(
        stored, fromBlockId, toBlockId
    );
    StyleAnalysisSnapshot snapshot = new StyleAnalysisSnapshot(
        novelId,
        revisionId,
        stored.contentHash(),
        profile.profileVersion(),
        lexicon,
        selected
    );
    String requestHash = CanonicalJson.hash(Map.of(
        "max_attempts", maxAttempts,
        "retention_seconds", retention.toSeconds(),
        "snapshot_hash", snapshot.snapshotHash()
    ));
    Instant createdAt = auditContext.occurredAt();
    StyleAnalysisJob job = StyleAnalysisJob.queued(
        Ids.JobId.create(),
        Ids.StyleAnalysisId.create(),
        snapshot,
        maxAttempts,
        idempotencyKey,
        requestHash,
        auditContext,
        createdAt.plus(retention),
        createdAt
    );
    return self.analyses.createStyleAnalysisJob(job);
  }
}
