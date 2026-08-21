package dev.storyblock.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.application.CommitService;
import dev.storyblock.application.CommitRejectedException;
import dev.storyblock.application.ReplayResult;
import dev.storyblock.application.ReplayService;
import dev.storyblock.application.ReplayVerificationReport;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.security.AccessKeyService;
import dev.storyblock.security.AccessScope;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;
import dev.storyblock.security.IssueAccessKeyCommand;
import dev.storyblock.storage.CommitRequest;
import dev.storyblock.storage.CommitResult;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StoredCheckpoint;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteRevisionStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void staleHeadWritesNothingAndCarriesHttp412Semantics() throws Exception {
        Path path = temporaryDirectory.resolve("stale.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            EditOperation first = RevisionStoreTestFixture.replace(
                    genesis, "first", "第二句。"
            );
            new CommitService(store).commit(
                    first, Ids.RevisionId.create(), Instant.parse("2026-08-21T12:01:00Z")
            );
            long revisions = store.revisionCount(genesis.novel().id());
            long operations = store.operationCount(genesis.novel().id());
            var head = store.getHead(genesis.novel().id());

            StaleHeadException failure = assertThrows(
                    StaleHeadException.class,
                    () -> new CommitService(store).commit(
                            RevisionStoreTestFixture.replace(
                                    genesis, "stale", "第三句。"
                            ),
                            Ids.RevisionId.create(),
                            Instant.parse("2026-08-21T12:02:00Z")
                    )
            );

            assertEquals(412, StaleHeadException.HTTP_STATUS);
            assertEquals(head, failure.actual());
            assertEquals(head, store.getHead(genesis.novel().id()));
            assertEquals(revisions, store.revisionCount(genesis.novel().id()));
            assertEquals(operations, store.operationCount(genesis.novel().id()));
        }
    }

    @Test
    void identicalRetryReturnsPriorResultAndChangedPayloadConflicts() throws Exception {
        Path path = temporaryDirectory.resolve("idempotency.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        EditOperation operation = RevisionStoreTestFixture.replace(
                genesis, "stable-key", "第二句。"
        );
        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            CommitService commits = new CommitService(store);
            CommitResult first = commits.commit(
                    operation,
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T12:01:00Z")
            );
            CommitResult retry = commits.commit(
                    operation,
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T13:01:00Z")
            );

            assertFalse(first.idempotentReplay());
            assertTrue(retry.idempotentReplay());
            assertEquals(first.revision(), retry.revision());
            assertEquals(first.operationId(), retry.operationId());
            assertEquals(2, store.revisionCount(genesis.novel().id()));
            assertEquals(1, store.operationCount(genesis.novel().id()));

            EditOperation changed = new EditOperation.ReplaceBlockRange(
                    operation.context(),
                    ((EditOperation.ReplaceBlockRange) operation).range(),
                    List.of(new dev.storyblock.domain.BlockDraft(
                            genesis.liveBlocks().getFirst().id(),
                            "不同內容。",
                            genesis.liveBlocks().getFirst().metadata(),
                            genesis.liveBlocks().getFirst().extensions()
                    ))
            );
            assertThrows(
                    IdempotencyConflictException.class,
                    () -> commits.commit(
                            changed,
                            Ids.RevisionId.create(),
                            Instant.parse("2026-08-21T14:01:00Z")
                    )
            );
            assertEquals(409, IdempotencyConflictException.HTTP_STATUS);
            assertEquals(2, store.revisionCount(genesis.novel().id()));
            assertEquals(1, store.operationCount(genesis.novel().id()));
        }
    }

    @Test
    void successfulAndIdempotentCommitsWriteRedactedAudits() throws Exception {
        Path path = temporaryDirectory.resolve("commit-audit.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        Instant issuedAt = Instant.parse("2026-08-21T12:00:00Z");
        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            var issued = new AccessKeyService(store, new byte[32]).issue(
                    new IssueAccessKeyCommand(
                            genesis.novel().id(),
                            "audit-actor",
                            Set.of(AccessScope.NOVEL_COMMIT),
                            issuedAt.plusSeconds(3600),
                            "issue-audit-key",
                            new AuditContext("req_issue_audit", "owner", null, issuedAt)
                    )
            );
            EditOperation operation = RevisionStoreTestFixture.replace(
                    genesis,
                    "commit-audit-key",
                    "Audit prose must remain outside event rows."
            );
            CommitService commits = new CommitService(store);

            CommitResult first = commits.commit(
                    operation,
                    Ids.RevisionId.create(),
                    issuedAt.plusSeconds(60),
                    new AuditContext(
                            "req_commit_first",
                            issued.key().actorId(),
                            issued.key().keyId(),
                            issuedAt.plusSeconds(60)
                    )
            );
            CommitResult retry = commits.commit(
                    operation,
                    Ids.RevisionId.create(),
                    issuedAt.plusSeconds(120),
                    new AuditContext(
                            "req_commit_retry",
                            issued.key().actorId(),
                            issued.key().keyId(),
                            issuedAt.plusSeconds(120)
                    )
            );

            List<AuditEvent> commitEvents = store.listAuditEvents(genesis.novel().id())
                    .stream()
                    .filter(event -> event.action() == AuditAction.COMMIT)
                    .toList();
            assertEquals(2, commitEvents.size());
            assertEquals(AuditResult.SUCCEEDED, commitEvents.get(0).result());
            assertEquals(AuditResult.IDEMPOTENT, commitEvents.get(1).result());
            assertEquals("req_commit_first", commitEvents.get(0).requestId());
            assertEquals("req_commit_retry", commitEvents.get(1).requestId());
            for (AuditEvent event : commitEvents) {
                assertEquals("audit-actor", event.actorId());
                assertEquals(issued.key().keyId(), event.actorKeyId());
                assertEquals(genesis.novel().id(), event.novelId());
                assertEquals(first.operationId(), event.operationId());
                assertEquals(first.revision().revisionId(), event.revisionId());
                assertEquals(71, event.operationHash().length());
                assertEquals(71, event.contentHash().length());
                assertEquals(71, event.eventHash().length());
                assertFalse(event.toString().contains("Audit prose"));
            }
            assertTrue(retry.idempotentReplay());
        }
    }

    @Test
    void deterministicValidationFailureNeverEntersTheWriteTransaction() throws Exception {
        Path path = temporaryDirectory.resolve("rejected.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));

            CommitRejectedException failure = assertThrows(
                    CommitRejectedException.class,
                    () -> new CommitService(store).commit(
                            RevisionStoreTestFixture.replace(
                                    genesis, "invalid", "未完成"
                            ),
                            Ids.RevisionId.create(),
                            Instant.parse("2026-08-21T12:01:00Z")
                    )
            );

            assertEquals(422, CommitRejectedException.HTTP_STATUS);
            assertFalse(failure.preview().committable());
            assertEquals(1, store.revisionCount(genesis.novel().id()));
            assertEquals(0, store.operationCount(genesis.novel().id()));
            assertEquals(genesis.id(), store.getHead(genesis.novel().id()).revisionId());
        }
    }

    @Test
    void everyInjectedCommitFailureRollsBackAllTablesAndHead() throws Exception {
        for (CommitStage stage : CommitStage.values()) {
            Path path = temporaryDirectory.resolve("rollback-" + stage + ".db");
            RevisionManifest genesis = RevisionStoreTestFixture.genesis();
            EditOperation operation = RevisionStoreTestFixture.replace(
                    genesis, "fault-" + stage, "第二句。"
            );
            CommitRequest request = RevisionStoreTestFixture.request(
                    genesis,
                    operation,
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T12:01:00Z")
            );
            try (SqliteRevisionStore store = SqliteRevisionStore.open(
                    path,
                    new CheckpointPolicy(1, 1),
                    current -> {
                        if (current == stage) {
                            throw new InjectedFailure(stage);
                        }
                    }
            )) {
                store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
                assertThrows(InjectedFailure.class, () -> store.commitCas(request), stage.name());
                assertEquals(1, store.revisionCount(genesis.novel().id()), stage.name());
                assertEquals(0, store.operationCount(genesis.novel().id()), stage.name());
                assertTrue(store.listTombstones(genesis.novel().id()).isEmpty(), stage.name());
                assertEquals(genesis.id(), store.getHead(genesis.novel().id()).revisionId());
            }
            assertEquals(1, count(path, "revisions"), stage.name());
            assertEquals(0, count(path, "operations"), stage.name());
            assertEquals(1, count(path, "checkpoints"), stage.name());
            assertEquals(1, count(path, "head_block_projection"), stage.name());
            assertEquals(0, count(path, "audit_events"), stage.name());
        }
    }

    @Test
    void checkpointPlusReplayAndFullReplayReproduceEveryHead() throws Exception {
        Path path = temporaryDirectory.resolve("replay.db");
        RevisionManifest firstNovel = RevisionStoreTestFixture.genesis();
        RevisionManifest secondNovel = RevisionStoreTestFixture.genesis();
        try (SqliteRevisionStore store = SqliteRevisionStore.open(
                path, new CheckpointPolicy(2, Long.MAX_VALUE)
        )) {
            store.createNovel(firstNovel, RevisionStoreTestFixture.hash(firstNovel));
            store.createNovel(secondNovel, RevisionStoreTestFixture.hash(secondNovel));
            CommitService commits = new CommitService(store);
            commitReplacement(store, commits, firstNovel.novel().id(), "one", "第二句。", 1);
            commitReplacement(store, commits, firstNovel.novel().id(), "two", "第三句。", 2);
            commitReplacement(store, commits, firstNovel.novel().id(), "three", "第四句。", 3);

            var head = store.getHead(firstNovel.novel().id());
            StoredCheckpoint checkpoint = store.loadCheckpoint(
                    firstNovel.novel().id(), head.sequence()
            ).orElseThrow();
            ReplayService replay = new ReplayService(store);
            ReplayResult materialized = replay.materialize(
                    firstNovel.novel().id(), head.revisionId()
            );
            ReplayResult full = replay.materializeFull(
                    firstNovel.novel().id(), head.revisionId()
            );
            ReplayVerificationReport report = replay.verifyAllHeads();

            assertEquals(2, checkpoint.sequence());
            assertEquals(GzipCheckpointCodec.NAME, checkpoint.codec());
            assertArrayEquals(
                    NarrativeCanonicalMapper.toCanonical(
                            store.getRevisionAtSequence(firstNovel.novel().id(), 2).manifest()
                    ).envelopeBytes(),
                    store.decompressCheckpoint(checkpoint)
            );
            assertEquals(2, materialized.startingSequence());
            assertEquals(1, materialized.replayedOperations());
            assertEquals(head.contentHash(), materialized.contentHash());
            assertEquals(0, full.startingSequence());
            assertEquals(3, full.replayedOperations());
            assertEquals(head.contentHash(), full.contentHash());
            assertTrue(report.valid());
            assertEquals(2, report.novels().size());
        }
    }

    @Test
    void replayPayloadThresholdCreatesCheckpointBeforeRevisionInterval() throws Exception {
        Path path = temporaryDirectory.resolve("payload-threshold.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        try (SqliteRevisionStore store = SqliteRevisionStore.open(
                path, new CheckpointPolicy(100, 1)
        )) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            new CommitService(store).commit(
                    RevisionStoreTestFixture.replace(
                            genesis, "payload-threshold", "第二句。"
                    ),
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T12:01:00Z")
            );

            StoredCheckpoint checkpoint = store.loadCheckpoint(
                    genesis.novel().id(), 1
            ).orElseThrow();
            assertEquals(1, checkpoint.sequence());
        }
    }

    @Test
    void malformedHeadIsReportedWithoutAbortingTheVerificationBatch() throws Exception {
        Path path = temporaryDirectory.resolve("malformed-head.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
        }
        try (SqliteDatabase database = SqliteDatabase.open(path)) {
            database.write(connection -> {
                try (var statement = connection.prepareStatement(
                        "UPDATE novels SET head_hash = ? WHERE novel_id = ?"
                )) {
                    statement.setString(1, "sha256:invalid");
                    statement.setString(2, genesis.novel().id().value());
                    statement.executeUpdate();
                }
                return null;
            });
        }

        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            ReplayVerificationReport report = new ReplayService(store).verifyAllHeads();

            assertFalse(report.valid());
            assertEquals(1, report.novels().size());
            assertEquals(genesis.novel().id(), report.novels().getFirst().novelId());
            assertNull(report.novels().getFirst().headRevisionId());
            assertTrue(report.novels().getFirst().detail().contains("content hash"));
        }
    }

    @Test
    void restoreCreatesANewRevisionAndNeverDeletesHistory() throws Exception {
        Path path = temporaryDirectory.resolve("restore.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        try (SqliteRevisionStore store = SqliteRevisionStore.open(
                path, new CheckpointPolicy(2, Long.MAX_VALUE)
        )) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            CommitService commits = new CommitService(store);
            CommitResult replacement = commits.commit(
                    RevisionStoreTestFixture.replace(genesis, "before-delete", "第二句。"),
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T12:01:00Z")
            );
            RevisionManifest revised = store.getRevision(
                    genesis.novel().id(), replacement.revision().revisionId()
            ).manifest();
            CommitResult deletion = commits.commit(
                    RevisionStoreTestFixture.delete(revised, "delete"),
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T12:02:00Z")
            );
            RevisionManifest deleted = store.getRevision(
                    genesis.novel().id(), deletion.revision().revisionId()
            ).manifest();
            assertTrue(deleted.liveBlocks().isEmpty());

            CommitResult restoration = commits.commit(
                    RevisionStoreTestFixture.restore(deleted, genesis, "restore"),
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T12:03:00Z")
            );
            RevisionManifest restored = store.getRevision(
                    genesis.novel().id(), restoration.revision().revisionId()
            ).manifest();

            assertNotEquals(genesis.id(), restored.id());
            assertNotEquals(deleted.id(), restored.id());
            assertEquals(deleted.id(), restored.parentId());
            assertEquals(genesis.novel(), restored.novel());
            assertEquals(genesis.selectedBlockVersions(), restored.selectedBlockVersions());
            assertEquals(4, store.revisionCount(genesis.novel().id()));
            assertEquals(3, store.operationCount(genesis.novel().id()));
            assertEquals(1, store.listTombstones(genesis.novel().id()).size());
            ReplayResult checkpointReplay = new ReplayService(store).materialize(
                    genesis.novel().id(), restored.id()
            );
            assertEquals(2, checkpointReplay.startingSequence());
            assertEquals(1, checkpointReplay.replayedOperations());
            assertEquals(restoration.revision().contentHash(), checkpointReplay.contentHash());
            assertEquals(
                    restoration.revision().contentHash(),
                    new ReplayService(store).materializeFull(
                            genesis.novel().id(), restored.id()
                    ).contentHash()
            );
        }

        assertAppendOnly(path, "operations");
        assertAppendOnly(path, "revisions");
        assertEquals(3, count(path, "operations"));
        assertEquals(4, count(path, "revisions"));
    }

    private static void commitReplacement(
            SqliteRevisionStore store,
            CommitService commits,
            Ids.NovelId novelId,
            String key,
            String text,
            int minute
    ) {
        var head = store.getHead(novelId);
        RevisionManifest base = store.getRevision(novelId, head.revisionId()).manifest();
        commits.commit(
                RevisionStoreTestFixture.replace(base, key, text),
                Ids.RevisionId.create(),
                Instant.parse("2026-08-21T12:0" + minute + ":00Z")
        );
    }

    private static long count(Path path, String table) throws Exception {
        if (!List.of(
                "revisions", "operations", "checkpoints", "head_block_projection",
                "audit_events"
        ).contains(table)) {
            throw new IllegalArgumentException("Unexpected test table " + table);
        }
        try (SqliteDatabase database = SqliteDatabase.open(path)) {
            return database.readOnly(connection -> {
                try (var statement = connection.createStatement();
                     var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    result.next();
                    return result.getLong(1);
                }
            });
        }
    }

    private static void assertAppendOnly(Path path, String table) throws Exception {
        try (SqliteDatabase database = SqliteDatabase.open(path)) {
            SQLException failure = assertThrows(SQLException.class, () ->
                    database.write(connection -> {
                        connection.createStatement().executeUpdate("DELETE FROM " + table);
                        return null;
                    })
            );
            assertTrue(failure.getMessage().contains("append-only"));
        }
    }

    private static final class InjectedFailure extends RuntimeException {
        private InjectedFailure(CommitStage stage) {
            super(stage.name());
        }
    }
}
