package dev.storyblock.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.application.MonitorService;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.monitor.MonitorBlockFingerprint;
import dev.storyblock.monitor.MonitorEvidence;
import dev.storyblock.monitor.MonitorFinding;
import dev.storyblock.monitor.MonitorModule;
import dev.storyblock.monitor.MonitorProposedOperation;
import dev.storyblock.monitor.MonitorRunState;
import dev.storyblock.monitor.MonitorStaleReason;
import dev.storyblock.monitor.StoredMonitorRun;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.validator.EvidenceSpans;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteMonitorStoreTest {
    private static final Instant NOW = Instant.parse("2026-08-21T13:00:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void monitorOutputsAreSeparateImmutableAndIdempotent() throws Exception {
        Path path = temporaryDirectory.resolve("monitor.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        StoredMonitorRun first = findingRun(genesis, "monitor-request", "LOCAL_NOTE");

        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));

            var inserted = store.saveMonitorRun(first);
            var replayed = store.saveMonitorRun(findingRun(
                    genesis, "monitor-request", "LOCAL_NOTE"
            ));

            assertFalse(inserted.idempotentReplay());
            assertTrue(replayed.idempotentReplay());
            assertEquals(first.runId(), replayed.run().runId());
            assertEquals(first.outputId(), replayed.run().outputId());
            assertEquals(first, store.getMonitorRun(genesis.novel().id(), first.runId()));
            assertThrows(IdempotencyConflictException.class, () ->
                    store.saveMonitorRun(findingRun(
                            genesis, "monitor-request", "DIFFERENT_NOTE"
                    ))
            );
        }

        try (SqliteDatabase database = SqliteDatabase.open(path)) {
            assertEquals(1, count(database, "monitor_runs"));
            assertEquals(1, count(database, "monitor_issues"));
            assertEquals(0, count(database, "monitor_proposed_operations"));
            SQLException update = assertThrows(SQLException.class, () ->
                    database.write(connection -> {
                        connection.createStatement().executeUpdate(
                                "UPDATE monitor_runs SET rule_version = 'changed'"
                        );
                        return null;
                    })
            );
            assertTrue(update.getMessage().contains("append-only"));
            SQLException delete = assertThrows(SQLException.class, () ->
                    database.write(connection -> {
                        connection.createStatement().executeUpdate(
                                "DELETE FROM monitor_issues"
                        );
                        return null;
                    })
            );
            assertTrue(delete.getMessage().contains("append-only"));
        }
    }

    @Test
    void relevantHeadAndBlockChangesMarkRunStaleWithoutRebase() throws Exception {
        Path path = temporaryDirectory.resolve("stale-monitor.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        String hash = RevisionStoreTestFixture.hash(genesis);
        NarrativeBlock block = genesis.liveBlocks().getFirst();

        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, hash);
            MonitorService service = new MonitorService(store, store);
            MonitorEvidence evidence = evidence(block);
            var submitted = service.submit(
                    genesis.novel().id(),
                    genesis.id(),
                    hash,
                    block.id(),
                    1,
                    MonitorModule.RULE_VERSION,
                    List.of(block.id()),
                    new MonitorFinding(
                            "LOCAL_NOTE", "info", "Check the opening.", List.of(evidence)
                    ),
                    "service-monitor-request",
                    new AuditContext("req_monitor", "monitor-worker", null, NOW)
            );
            assertEquals(MonitorRunState.CURRENT, submitted.status().state());
            assertFalse((Boolean) submitted.status().canonicalValue().get("rebase_allowed"));

            EditOperation replacement = RevisionStoreTestFixture.replace(
                    genesis, "replace-after-monitor", "更新句。"
            );
            var commit = RevisionStoreTestFixture.request(
                    genesis, replacement, Ids.RevisionId.create(), NOW.plusSeconds(1)
            );
            store.commitCas(commit);

            var stale = service.getStatus(
                    genesis.novel().id(), submitted.status().run().runId()
            );
            assertEquals(MonitorRunState.STALE, stale.state());
            assertTrue(stale.staleReasons().contains(MonitorStaleReason.HEAD_CHANGED));
            assertTrue(stale.staleReasons().contains(
                    MonitorStaleReason.AFFECTED_BLOCK_CHANGED
            ));
            assertFalse((Boolean) stale.canonicalValue().get("rebase_allowed"));
            assertEquals(genesis.id(), stale.run().revisionId());
        }
    }

    @Test
    void ruleVersionChangeInvalidatesWithoutChangingTheStoredFinding() throws Exception {
        Path path = temporaryDirectory.resolve("rule-stale-monitor.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        StoredMonitorRun oldRule = findingRun(
                genesis, "old-rule-monitor", "LOCAL_NOTE", "monitor-rules-0.9.0"
        );

        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            store.saveMonitorRun(oldRule);

            var status = new MonitorService(store, store).getStatus(
                    genesis.novel().id(), oldRule.runId()
            );

            assertEquals(MonitorRunState.STALE, status.state());
            assertEquals(
                    List.of(MonitorStaleReason.RULE_VERSION_CHANGED),
                    status.staleReasons()
            );
            assertEquals(oldRule, status.run());
            assertFalse((Boolean) status.canonicalValue().get("rebase_allowed"));
        }
    }

    @Test
    void proposedOperationIsPersistedButNeverExecuted() throws Exception {
        Path path = temporaryDirectory.resolve("monitor-proposal.db");
        RevisionManifest genesis = RevisionStoreTestFixture.genesis();
        NarrativeBlock block = genesis.liveBlocks().getFirst();
        String hash = RevisionStoreTestFixture.hash(genesis);
        EditOperation proposal = RevisionStoreTestFixture.replace(
                genesis, "inert-proposal", "建議句。"
        );

        try (SqliteRevisionStore store = SqliteRevisionStore.open(path)) {
            store.createNovel(genesis, hash);
            MonitorService service = new MonitorService(store, store);

            var submitted = service.submit(
                    genesis.novel().id(),
                    genesis.id(),
                    hash,
                    block.id(),
                    1,
                    MonitorModule.RULE_VERSION,
                    List.of(block.id()),
                    new MonitorProposedOperation(proposal, List.of(evidence(block))),
                    "proposal-submission",
                    new AuditContext("req_proposal", "monitor-worker", null, NOW)
            );

            assertTrue(submitted.status().run().output()
                    instanceof MonitorProposedOperation);
            assertEquals(1, store.revisionCount(genesis.novel().id()));
            assertEquals(0, store.operationCount(genesis.novel().id()));
            assertEquals(genesis.id(), store.getHead(genesis.novel().id()).revisionId());

            EditOperation restore = RevisionStoreTestFixture.restore(
                    genesis, genesis, "forbidden-restore-proposal"
            );
            assertThrows(IllegalArgumentException.class, () -> service.submit(
                    genesis.novel().id(),
                    genesis.id(),
                    hash,
                    block.id(),
                    1,
                    MonitorModule.RULE_VERSION,
                    List.of(block.id()),
                    new MonitorProposedOperation(restore, List.of(evidence(block))),
                    "restore-submission",
                    new AuditContext("req_restore", "monitor-worker", null, NOW)
            ));
        }

        try (SqliteDatabase database = SqliteDatabase.open(path)) {
            assertEquals(1, count(database, "monitor_runs"));
            assertEquals(0, count(database, "monitor_issues"));
            assertEquals(1, count(database, "monitor_proposed_operations"));
        }
    }

    private static StoredMonitorRun findingRun(
            RevisionManifest revision,
            String idempotencyKey,
            String code
    ) {
        return findingRun(
                revision, idempotencyKey, code, MonitorModule.RULE_VERSION
        );
    }

    private static StoredMonitorRun findingRun(
            RevisionManifest revision,
            String idempotencyKey,
            String code,
            String ruleVersion
    ) {
        NarrativeBlock block = revision.liveBlocks().getFirst();
        String revisionHash = RevisionStoreTestFixture.hash(revision);
        MonitorFinding finding = new MonitorFinding(
                code, "info", "A local monitor note.", List.of(evidence(block))
        );
        List<MonitorBlockFingerprint> affected = List.of(
                MonitorBlockFingerprint.from(block)
        );
        String requestHash = StoredMonitorRun.requestHash(
                revision.novel().id(),
                revision.id(),
                revisionHash,
                block.id(),
                1,
                MonitorModule.VERSION,
                ruleVersion,
                affected,
                finding
        );
        return new StoredMonitorRun(
                Ids.MonitorRunId.create(),
                Ids.MonitorIssueId.create(),
                revision.novel().id(),
                revision.id(),
                revisionHash,
                block.id(),
                1,
                MonitorModule.VERSION,
                ruleVersion,
                affected,
                finding,
                idempotencyKey,
                requestHash,
                new AuditContext("req_" + idempotencyKey, "monitor-worker", null, NOW),
                NOW
        );
    }

    private static MonitorEvidence evidence(NarrativeBlock block) {
        String quote = block.text().substring(0, 1);
        return new MonitorEvidence(
                block.id(), 0, 1, quote, EvidenceSpans.quoteHash(quote)
        );
    }

    private static long count(SqliteDatabase database, String table) throws SQLException {
        if (!List.of(
                "monitor_runs", "monitor_issues", "monitor_proposed_operations"
        ).contains(table)) {
            throw new IllegalArgumentException("Unexpected monitor table " + table);
        }
        return database.readOnly(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                result.next();
                return result.getLong(1);
            }
        });
    }
}
