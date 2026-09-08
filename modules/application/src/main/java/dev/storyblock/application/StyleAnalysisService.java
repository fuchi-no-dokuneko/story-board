package dev.storyblock.application;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StoredRevision;
import dev.storyblock.style.ExpiredStyleArtifactException;
import dev.storyblock.style.StyleAnalysisClaimCommand;
import dev.storyblock.style.StyleAnalysisCompletionCommand;
import dev.storyblock.style.StyleAnalysisCompletionResult;
import dev.storyblock.style.StyleAnalysisExecution;
import dev.storyblock.style.StyleAnalysisExecutor;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisJobSaveResult;
import dev.storyblock.style.StyleAnalysisLease;
import dev.storyblock.style.StyleAnalysisResult;
import dev.storyblock.style.StyleAnalysisSnapshot;
import dev.storyblock.style.StyleAnalysisSnapshotConflictException;
import dev.storyblock.style.StyleAnalysisStore;
import dev.storyblock.style.StyleAnalysisTrace;
import dev.storyblock.style.StyleAnalysisWindowPage;
import dev.storyblock.style.StyleAnalysisWindowSlice;
import dev.storyblock.style.StyleAnalysisBlock;
import dev.storyblock.style.StyleLifecycleConflictException;
import dev.storyblock.style.StyleMaskingLexicon;
import dev.storyblock.style.StyleProfileStore;
import dev.storyblock.style.StyleProfileVersionView;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class StyleAnalysisService {
    public static final Duration DEFAULT_RETENTION = Duration.ofDays(30);
    public static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final RevisionStore revisions;
    private final StyleProfileStore profiles;
    private final StyleAnalysisStore analyses;
    private final StyleAnalysisExecutor executor;

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
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(profileVersionId, "profileVersionId");
        Objects.requireNonNull(lexicon, "lexicon");
        Objects.requireNonNull(auditContext, "auditContext");
        StyleAnalysisServiceValidateRetention.validateRetention(retention);

        StoredRevision stored = revisions.getRevision(novelId, revisionId);
        if (!stored.contentHash().equals(expectedRevisionHash)) {
            throw new StyleAnalysisSnapshotConflictException(stored.contentHash());
        }
        StyleProfileVersionView profile = profiles.getStyleProfileVersion(
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
        return analyses.createStyleAnalysisJob(job);
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
        Objects.requireNonNull(lease, "lease");
        StyleAnalysisExecution execution = executor.execute(lease.snapshot());
        StyleAnalysisTrace trace = StyleAnalysisTrace.create(
                lease.analysisId(),
                execution.tracePayload(),
                completedAt,
                lease.retentionUntil()
        );
        return complete(new StyleAnalysisCompletionCommand(
                lease.jobId(),
                lease.leaseOwner(),
                lease.attempt(),
                lease.claimedStatusHash(),
                lease.snapshot().snapshotHash(),
                lease.snapshot().profileVersionHash(),
                lease.snapshot().analyzerContractHash(),
                lease.snapshot().windowConfigurationHash(),
                execution.summary(),
                execution.windows(),
                trace,
                idempotencyKey,
                completedAt
        ));
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
        int after = cursor == null ? -1 : StyleAnalysisServiceDecodeCursor.decodeCursor(analysisId, cursor);
        StyleAnalysisWindowSlice slice = analyses.listStyleAnalysisWindows(
                analysisId, after, limit
        );
        String next = slice.nextOrdinal() == null
                ? null : StyleAnalysisServiceEncodeCursor.encodeCursor(analysisId, slice.nextOrdinal());
        return new StyleAnalysisWindowPage(analysisId, slice.items(), next);
    }

    public void requireArtifactAvailable(Ids.ArtifactId artifactId, Instant now) {
        analyses.findStyleArtifactExpiry(artifactId).ifPresent(expiry -> {
            if (!now.isBefore(expiry)) {
                throw new ExpiredStyleArtifactException(artifactId);
            }
        });
    }

}
