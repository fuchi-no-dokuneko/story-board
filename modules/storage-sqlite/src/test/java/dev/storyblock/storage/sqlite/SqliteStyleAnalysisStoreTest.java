package dev.storyblock.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.application.StyleProfileService;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.security.AuditContext;
import dev.storyblock.style.StyleAnalysisBlock;
import dev.storyblock.style.StyleAnalysisClaimCommand;
import dev.storyblock.style.StyleAnalysisCompletionCommand;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisJobStatus;
import dev.storyblock.style.StyleAnalysisLeaseConflictException;
import dev.storyblock.style.StyleAnalysisResultConflictException;
import dev.storyblock.style.StyleAnalysisSnapshot;
import dev.storyblock.style.StyleAnalysisSummary;
import dev.storyblock.style.StyleAnalysisTrace;
import dev.storyblock.style.StyleDecisionState;
import dev.storyblock.style.StyleMaskingLexicon;
import dev.storyblock.style.StyleProfileScope;
import dev.storyblock.style.StyleScopeKind;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteStyleAnalysisStoreTest {
    private static final Instant NOW = Instant.parse("2026-08-21T15:00:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void expiredLeaseIsReclaimedAndCanonicalResultIsCommittedOnce() throws Exception {
        Path path = temporaryDirectory.resolve("analysis.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        Ids.NovelId novelId = genesis.novel().id();

        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            String revisionHash = RevisionStoreTestFixture.hash(genesis);
            store.createNovel(genesis, revisionHash);
            StyleProfileService profiles = new StyleProfileService(store);
            var profile = profiles.createProfile(
                    "Analysis baseline",
                    new StyleProfileScope(novelId, StyleScopeKind.NOVEL, null),
                    "Owner test corpus",
                    "analysis-profile",
                    audit(0, "profile")
            );
            var version = profiles.createVersion(
                    profile.profile().profileId(),
                    SqliteStyleProfileStoreTest.content(
                            genesis,
                            profile.profile().scope(),
                            dev.storyblock.style.StyleCorpusSourceKind.OWNER
                    ),
                    profile.profile().resourceHash(),
                    "analysis-version",
                    audit(1, "version")
            ).view().profileVersion();
            NarrativeScene scene = genesis.novel().chapters().getFirst()
                    .scenes().getFirst();
            StyleAnalysisSnapshot snapshot = new StyleAnalysisSnapshot(
                    novelId,
                    genesis.id(),
                    revisionHash,
                    version,
                    StyleMaskingLexicon.empty(),
                    scene.blocks().stream()
                            .map(block -> StyleAnalysisBlock.from(scene, block))
                            .toList()
            );
            AuditContext requested = audit(2, "analysis");
            StyleAnalysisJob queued = StyleAnalysisJob.queued(
                    Ids.JobId.create(),
                    Ids.StyleAnalysisId.create(),
                    snapshot,
                    3,
                    "analysis-create",
                    CanonicalJson.hash("analysis-request"),
                    requested,
                    requested.occurredAt().plus(Duration.ofDays(30)),
                    requested.occurredAt()
            );

            var created = store.createStyleAnalysisJob(queued);
            var replay = store.createStyleAnalysisJob(queued);
            assertFalse(created.idempotentReplay());
            assertTrue(replay.idempotentReplay());

            var firstLease = claim(
                    store,
                    novelId,
                    "worker-a",
                    "claim-a",
                    NOW.plusSeconds(2).plusMillis(100)
            );
            var claimReplay = claim(
                    store, novelId, "worker-a", "claim-a", NOW.plusSeconds(4)
            );
            assertEquals(firstLease.jobId(), claimReplay.jobId());
            assertEquals(firstLease.attempt(), claimReplay.attempt());
            assertTrue(claimReplay.idempotentReplay());

            var reclaimed = claim(
                    store, novelId, "worker-b", "claim-b", NOW.plusSeconds(40)
            );
            assertEquals(2, reclaimed.attempt());
            assertThrows(StyleAnalysisLeaseConflictException.class, () ->
                    store.completeStyleAnalysis(completion(
                            firstLease, "late-first-worker", NOW.plusSeconds(41), "first"
                    ))
            );
            assertThrows(StyleAnalysisLeaseConflictException.class, () ->
                    store.completeStyleAnalysis(completion(
                            reclaimed,
                            "exact-expiry",
                            reclaimed.leaseUntil(),
                            "expired"
                    ))
            );

            StyleAnalysisCompletionCommand completion = completion(
                    reclaimed, "complete-b", NOW.plusSeconds(42), "canonical"
            );
            var completed = store.completeStyleAnalysis(completion);
            assertFalse(completed.idempotentReplay());
            assertEquals(StyleAnalysisJobStatus.SUCCEEDED, completed.job().status());
            assertEquals(completion.resultHash(), completed.result().resultHash());
            assertTrue(store.completeStyleAnalysis(completion).idempotentReplay());
            assertThrows(StyleAnalysisResultConflictException.class, () ->
                    store.completeStyleAnalysis(completion(
                            reclaimed,
                            "different-result",
                            NOW.plusSeconds(42),
                            "different"
                    ))
            );
            assertEquals(
                    completed.result(),
                    store.findStyleAnalysisResult(queued.analysisId()).orElseThrow()
            );
            assertTrue(store.listStyleAnalysisWindows(
                    queued.analysisId(), -1, 20
            ).items().isEmpty());
            assertEquals(
                    queued.retentionUntil(),
                    store.findStyleArtifactExpiry(
                            completed.result().traceArtifactId()
                    ).orElseThrow()
            );
        }

        try (SqliteDatabase database = SqliteDatabase.open(path)) {
            assertEquals(1, count(database, "analysis_jobs"));
            assertEquals(2, count(database, "analysis_claim_receipts"));
            assertEquals(1, count(database, "analysis_runs"));
            assertEquals(0, count(database, "analysis_window_findings"));
            assertEquals(1, count(database, "analysis_artifacts"));
            assertEquals(1, countWhere(
                    database, "artifacts", "kind = 'style-analysis-trace'"
            ));
            SQLException terminalUpdate = assertThrows(SQLException.class, () ->
                    database.write(connection -> {
                        connection.createStatement().executeUpdate(
                                "UPDATE analysis_jobs SET updated_at = updated_at"
                        );
                        return null;
                    })
            );
            assertTrue(terminalUpdate.getMessage().contains("terminal"));
        }
    }

    private static dev.storyblock.style.StyleAnalysisLease claim(
            SqliteRevisionStore store,
            Ids.NovelId novelId,
            String owner,
            String key,
            Instant claimedAt
    ) {
        Duration duration = Duration.ofSeconds(30);
        return store.claimStyleAnalysis(new StyleAnalysisClaimCommand(
                novelId,
                owner,
                duration,
                key,
                StyleAnalysisClaimCommand.hash(novelId, owner, duration),
                claimedAt
        )).orElseThrow();
    }

    private static StyleAnalysisCompletionCommand completion(
            dev.storyblock.style.StyleAnalysisLease lease,
            String idempotencyKey,
            Instant completedAt,
            String traceMarker
    ) {
        StyleAnalysisSummary summary = new StyleAnalysisSummary(
                lease.snapshot().blocks().size(),
                0,
                0,
                Map.of(StyleDecisionState.NORMAL, 0)
        );
        StyleAnalysisTrace trace = StyleAnalysisTrace.create(
                lease.analysisId(),
                Map.of(
                        "marker", traceMarker,
                        "token_trace", List.of("trace-value".repeat(10_000))
                ),
                completedAt,
                lease.retentionUntil()
        );
        return new StyleAnalysisCompletionCommand(
                lease.jobId(),
                lease.leaseOwner(),
                lease.attempt(),
                lease.claimedStatusHash(),
                lease.snapshot().snapshotHash(),
                lease.snapshot().profileVersionHash(),
                lease.snapshot().analyzerContractHash(),
                lease.snapshot().windowConfigurationHash(),
                summary,
                List.of(),
                trace,
                idempotencyKey,
                completedAt
        );
    }

    private static AuditContext audit(int seconds, String request) {
        return new AuditContext(
                "req_analysis_" + request,
                "analysis-owner",
                null,
                NOW.plusSeconds(seconds)
        );
    }

    private static long count(SqliteDatabase database, String table) throws Exception {
        return countWhere(database, table, "1 = 1");
    }

    private static long countWhere(
            SqliteDatabase database,
            String table,
            String predicate
    ) throws Exception {
        if (!List.of(
                "analysis_jobs",
                "analysis_claim_receipts",
                "analysis_runs",
                "analysis_window_findings",
                "analysis_artifacts",
                "artifacts"
        ).contains(table)) {
            throw new IllegalArgumentException("Unexpected analysis table " + table);
        }
        return database.readOnly(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT COUNT(*) FROM " + table + " WHERE " + predicate
                 )) {
                result.next();
                return result.getLong(1);
            }
        });
    }
}
