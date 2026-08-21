package dev.storyblock.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.application.CanonicalTransferService;
import dev.storyblock.application.CommitService;
import dev.storyblock.application.ReplayService;
import dev.storyblock.contracts.CanonicalExportFormat;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.CanonicalPackageException;
import dev.storyblock.contracts.CanonicalRevision;
import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.storage.CanonicalImportRequest;
import dev.storyblock.storage.ExportJobRequest;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.storage.MissingArtifactException;
import dev.storyblock.storage.StoredArtifact;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteCanonicalTransferTest {
    private static final Instant IMPORTED_AT = Instant.parse("2026-08-21T13:00:00Z");
    private static final List<String> IMPORT_TABLES = List.of(
            "novels",
            "revisions",
            "operations",
            "artifacts",
            "import_receipts",
            "checkpoints",
            "head_block_projection",
            "block_tombstones"
    );

    @TempDir
    Path temporaryDirectory;

    @Test
    void completePackageRoundTripsWithHistoryArtifactTombstoneAndHeadHash()
            throws Exception {
        CanonicalNovelPackage sourceDocument = packageWithHistoryAndArtifact(
                temporaryDirectory.resolve("source.db")
        );
        Path targetPath = temporaryDirectory.resolve("target.db");
        try (SqliteRevisionStore target = SqliteRevisionStore.open(targetPath)) {
            CanonicalTransferService transfers = new CanonicalTransferService(target);

            var imported = transfers.importDocument(
                    CanonicalExportFormat.PACKAGE,
                    sourceDocument.bytes(),
                    "import-complete",
                    IMPORTED_AT
            );
            var retry = transfers.importDocument(
                    CanonicalExportFormat.PACKAGE,
                    sourceDocument.bytes(),
                    "import-complete",
                    IMPORTED_AT.plusSeconds(30)
            );
            byte[] exported = transfers.exportPackage(sourceDocument.manifest().novelId());

            assertFalse(imported.idempotentReplay());
            assertTrue(retry.idempotentReplay());
            assertEquals(imported.head(), retry.head());
            assertEquals(sourceDocument.manifest().headHash(), imported.head().contentHash());
            assertArrayEquals(sourceDocument.bytes(), exported);
            assertEquals(3, target.revisionCount(imported.novelId()));
            assertEquals(2, target.operationCount(imported.novelId()));
            assertEquals(1, target.listTombstones(imported.novelId()).size());
            assertEquals(
                    sourceDocument.manifest().headHash(),
                    new ReplayService(target).materializeFull(
                            imported.novelId(), imported.head().revisionId()
                    ).contentHash()
            );
        }
    }

    @Test
    void everyInjectedImportFailureRollsBackEveryAffectedTable() throws Exception {
        CanonicalNovelPackage document = packageWithHistoryAndArtifact(
                temporaryDirectory.resolve("fault-source.db")
        );
        for (ImportStage stage : ImportStage.values()) {
            Path path = temporaryDirectory.resolve("import-fault-" + stage + ".db");
            try (SqliteRevisionStore store = SqliteRevisionStore.open(
                    path,
                    CheckpointPolicy.DEFAULT,
                    CommitFaultInjector.NONE,
                    current -> {
                        if (current == stage) {
                            throw new InjectedImportFailure(stage);
                        }
                    }
            )) {
                CanonicalTransferService transfers = new CanonicalTransferService(store);
                assertThrows(
                        InjectedImportFailure.class,
                        () -> transfers.importDocument(
                                CanonicalExportFormat.PACKAGE,
                                document.bytes(),
                                "fault-" + stage,
                                IMPORTED_AT
                        ),
                        stage.name()
                );
                assertTrue(store.listNovels().isEmpty(), stage.name());
            }
            for (String table : IMPORT_TABLES) {
                assertEquals(0, count(path, table), stage + " " + table);
            }
        }
    }

    @Test
    void malformedVersionAndSemanticReplayDriftNeverReachStorage() throws Exception {
        CanonicalNovelPackage valid = packageWithOneOperation(
                temporaryDirectory.resolve("invalid-source.db")
        );
        byte[] unsupported = new String(valid.bytes(), StandardCharsets.UTF_8)
                .replace("\"package_version\":\"1.0.0\"", "\"package_version\":\"9.0.0\"")
                .getBytes(StandardCharsets.UTF_8);
        CanonicalNovelPackage drifted = packageWithReplayDrift(valid);
        Path targetPath = temporaryDirectory.resolve("invalid-target.db");
        try (SqliteRevisionStore target = SqliteRevisionStore.open(targetPath)) {
            CanonicalTransferService transfers = new CanonicalTransferService(target);

            assertThrows(
                    CanonicalPackageException.class,
                    () -> transfers.importDocument(
                            CanonicalExportFormat.PACKAGE,
                            unsupported,
                            "unsupported",
                            IMPORTED_AT
                    )
            );
            assertThrows(
                    CanonicalPackageException.class,
                    () -> transfers.importDocument(
                            CanonicalExportFormat.PACKAGE,
                            drifted.bytes(),
                            "drifted",
                            IMPORTED_AT
                    )
            );
            assertTrue(target.listNovels().isEmpty());
        }
    }

    @Test
    void importAndExportIdempotencyRejectChangedPayloads() throws Exception {
        CanonicalNovelPackage first = packageWithOneOperation(
                temporaryDirectory.resolve("idempotency-source.db")
        );
        CanonicalRevision otherGenesis = NarrativeCanonicalMapper.toCanonical(
                RevisionStoreTestFixture.genesis()
        );
        Path targetPath = temporaryDirectory.resolve("idempotency-target.db");
        try (SqliteRevisionStore target = SqliteRevisionStore.open(targetPath)) {
            CanonicalTransferService transfers = new CanonicalTransferService(target);
            var imported = transfers.importDocument(
                    CanonicalExportFormat.PACKAGE,
                    first.bytes(),
                    "same-import-key",
                    IMPORTED_AT
            );

            assertThrows(
                    IdempotencyConflictException.class,
                    () -> transfers.importDocument(
                            CanonicalExportFormat.REVISION,
                            otherGenesis.envelopeBytes(),
                            "same-import-key",
                            IMPORTED_AT
                    )
            );

            var head = target.getHead(first.manifest().novelId());
            var exported = transfers.requestExport(
                    first.manifest().novelId(),
                    head.revisionId(),
                    head.contentHash(),
                    CanonicalExportFormat.PACKAGE,
                    "same-export-key",
                    Instant.parse("2026-08-21T14:00:00Z")
            );
            var retry = transfers.requestExport(
                    first.manifest().novelId(),
                    head.revisionId(),
                    head.contentHash(),
                    CanonicalExportFormat.PACKAGE,
                    "same-export-key",
                    Instant.parse("2026-08-21T14:01:00Z")
            );

            assertFalse(exported.idempotentReplay());
            assertTrue(retry.idempotentReplay());
            assertEquals(exported.job(), retry.job());
            assertArrayEquals(
                    first.bytes(),
                    transfers.getArtifact(exported.job().resultArtifactId()).content()
            );
            assertArrayEquals(first.bytes(), transfers.exportPackage(first.manifest().novelId()));
            assertThrows(
                    IdempotencyConflictException.class,
                    () -> transfers.requestExport(
                            first.manifest().novelId(),
                            head.revisionId(),
                            head.contentHash(),
                            CanonicalExportFormat.REVISION,
                            "same-export-key",
                            Instant.parse("2026-08-21T14:02:00Z")
                    )
            );

            RevisionManifest importedHead = target.getRevision(
                    imported.novelId(), imported.head().revisionId()
            ).manifest();
            var advanced = new CommitService(target).commit(
                    RevisionStoreTestFixture.replace(
                            importedHead, "after-import", "第三句。"
                    ),
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T14:03:00Z")
            );
            var importRetry = transfers.importDocument(
                    CanonicalExportFormat.PACKAGE,
                    first.bytes(),
                    "same-import-key",
                    Instant.parse("2026-08-21T14:04:00Z")
            );

            assertTrue(importRetry.idempotentReplay());
            assertEquals(imported.head(), importRetry.head());
            assertFalse(importRetry.head().equals(advanced.revision()));
        }
    }

    @Test
    void failedJobInsertRollsBackItsGeneratedArtifact() throws Exception {
        CanonicalNovelPackage document = packageWithOneOperation(
                temporaryDirectory.resolve("job-source.db")
        );
        Path targetPath = temporaryDirectory.resolve("job-target.db");
        try (SqliteRevisionStore target = SqliteRevisionStore.open(targetPath)) {
            CanonicalTransferService transfers = new CanonicalTransferService(target);
            transfers.importDocument(
                    CanonicalExportFormat.PACKAGE,
                    document.bytes(),
                    "job-import",
                    IMPORTED_AT
            );
            var head = target.getHead(document.manifest().novelId());
            var first = transfers.requestExport(
                    document.manifest().novelId(),
                    head.revisionId(),
                    head.contentHash(),
                    CanonicalExportFormat.PACKAGE,
                    "job-one",
                    Instant.parse("2026-08-21T14:00:00Z")
            );
            Ids.ArtifactId rolledBackArtifactId = Ids.ArtifactId.create();
            byte[] content = document.bytes();
            StoredArtifact artifact = new StoredArtifact(
                    rolledBackArtifactId,
                    document.manifest().novelId(),
                    head.revisionId(),
                    "canonical-package",
                    "application/vnd.storyblock.package+json",
                    "identity",
                    CanonicalJson.hashBytes(content),
                    content,
                    Instant.parse("2026-08-21T14:01:00Z"),
                    false
            );
            ExportJobRequest duplicateJob = new ExportJobRequest(
                    first.job().jobId(),
                    document.manifest().novelId(),
                    head,
                    CanonicalExportFormat.PACKAGE,
                    "job-two",
                    CanonicalJson.hash(Map.of("request", "job-two")),
                    artifact,
                    artifact.createdAt()
            );

            assertThrows(
                    dev.storyblock.storage.StorageException.class,
                    () -> target.createCompletedExport(duplicateJob)
            );
            assertThrows(
                    MissingArtifactException.class,
                    () -> target.getArtifact(rolledBackArtifactId)
            );
            assertArrayEquals(
                    content,
                    target.getArtifact(first.job().resultArtifactId()).content()
            );
        }
    }

    @Test
    void generatedExportMayExceedOnePortableArtifactLimit() {
        byte[] content = new byte[CanonicalNovelPackage.MAX_ARTIFACT_BYTES + 1];
        String hash = CanonicalJson.hashBytes(content);

        StoredArtifact generated = new StoredArtifact(
                Ids.ArtifactId.create(),
                Ids.NovelId.create(),
                Ids.RevisionId.create(),
                "canonical-package",
                "application/vnd.storyblock.package+json",
                "identity",
                hash,
                content,
                IMPORTED_AT,
                false
        );

        assertEquals(content.length, generated.content().length);
        assertThrows(
                CanonicalPackageException.class,
                () -> new StoredArtifact(
                        Ids.ArtifactId.create(),
                        Ids.NovelId.create(),
                        Ids.RevisionId.create(),
                        "portable-trace",
                        "application/octet-stream",
                        "identity",
                        hash,
                        content,
                        IMPORTED_AT,
                        true
                )
        );
    }

    private CanonicalNovelPackage packageWithHistoryAndArtifact(Path path) throws Exception {
        CanonicalNovelPackage history;
        try (SqliteRevisionStore source = SqliteRevisionStore.open(path)) {
            RevisionManifest genesis = RevisionStoreTestFixture.genesis();
            source.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            CommitService commits = new CommitService(source);
            var replacement = commits.commit(
                    RevisionStoreTestFixture.replace(genesis, "package-replace", "第二句。"),
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T12:01:00Z")
            );
            RevisionManifest revised = source.getRevision(
                    genesis.novel().id(), replacement.revision().revisionId()
            ).manifest();
            commits.commit(
                    RevisionStoreTestFixture.delete(revised, "package-delete"),
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T12:02:00Z")
            );
            history = source.loadCanonicalPackage(genesis.novel().id());
        }
        byte[] trace = "portable detector trace".getBytes(StandardCharsets.UTF_8);
        return CanonicalNovelPackage.assemble(
                history.revisions(),
                history.operations(),
                List.of(new CanonicalNovelPackage.ArtifactEntry(
                        Ids.ArtifactId.create(),
                        history.manifest().headRevisionId(),
                        "detector-trace",
                        "application/json",
                        "identity",
                        CanonicalJson.hashBytes(trace),
                        trace,
                        Instant.parse("2026-08-21T12:03:00Z")
                ))
        );
    }

    private CanonicalNovelPackage packageWithOneOperation(Path path) throws Exception {
        try (SqliteRevisionStore source = SqliteRevisionStore.open(path)) {
            RevisionManifest genesis = RevisionStoreTestFixture.genesis();
            source.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            new CommitService(source).commit(
                    RevisionStoreTestFixture.replace(genesis, "one-replace", "第二句。"),
                    Ids.RevisionId.create(),
                    Instant.parse("2026-08-21T12:01:00Z")
            );
            return source.loadCanonicalPackage(genesis.novel().id());
        }
    }

    private static CanonicalNovelPackage packageWithReplayDrift(
            CanonicalNovelPackage valid
    ) {
        CanonicalNovelPackage.RevisionEntry originalResult = valid.revisions().get(1);
        RevisionManifest result = NarrativeCanonicalMapper.fromCanonical(originalResult.revision());
        NarrativeChapter chapter = result.novel().chapters().getFirst();
        NarrativeScene scene = chapter.scenes().getFirst();
        NarrativeBlock block = scene.blocks().getFirst();
        NarrativeBlock driftedBlock = block.revise(
                "第三句。",
                block.metadata(),
                block.extensions(),
                Ids.BlockVersionId.create()
        );
        NarrativeChapter driftedChapter = chapter.withScenes(List.of(
                scene.withBlocks(List.of(driftedBlock))
        ));
        RevisionManifest driftedManifest = new RevisionManifest(
                result.id(),
                result.parentId(),
                result.createdAt(),
                new NarrativeNovel(
                        result.novel().id(),
                        List.of(driftedChapter),
                        result.novel().extensions()
                )
        );
        CanonicalRevision driftedRevision = NarrativeCanonicalMapper.toCanonical(
                driftedManifest
        );
        var originalOperation = valid.operations().getFirst();
        var driftedOperation = new CanonicalNovelPackage.OperationEntry(
                originalOperation.sequence(),
                EditOperationCanonicalMapper.hash(originalOperation.operation()),
                originalOperation.operation(),
                driftedManifest.id(),
                driftedRevision.contentHash(),
                originalOperation.committedAt()
        );
        return CanonicalNovelPackage.assemble(
                List.of(valid.revisions().getFirst(), new CanonicalNovelPackage.RevisionEntry(
                        1, driftedRevision
                )),
                List.of(driftedOperation),
                List.of()
        );
    }

    private static long count(Path path, String table) throws Exception {
        if (!IMPORT_TABLES.contains(table)) {
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

    private static final class InjectedImportFailure extends RuntimeException {
        private InjectedImportFailure(ImportStage stage) {
            super(stage.name());
        }
    }
}
