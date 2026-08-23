package dev.storyblock.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.application.CommitService;
import dev.storyblock.application.NarrativeEditor;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.detector.AdjacentMetadataDetector;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.BlockVersionRef;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneSeed;
import dev.storyblock.domain.TransitionMode;
import dev.storyblock.renderer.DeterministicRenderer;
import dev.storyblock.renderer.RenderRange;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.CommitRequest;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import dev.storyblock.storage.sqlite.SqliteWalSpike;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class InitialSloQualificationTest {
    private static final int BLOCK_COUNT = 1_000;
    private static final String NFR_001 = "NFR-001";
    private static final String NFR_002 = "NFR-002";
    private static final String NFR_003 = "NFR-003";
    private static final String NFR_004 = "NFR-004";
    private static final String NFR_005 = "NFR-005";
    private static final String NFR_009 = "NFR-009";

    @Test
    void qualifiesCoreInitialSlosOnDeclaredHardware() throws Exception {
        Path outputDirectory = Path.of("target/slo").toAbsolutePath().normalize();
        Files.createDirectories(outputDirectory);
        Path databasePath = outputDirectory.resolve("storyblock.db");
        Path walPath = outputDirectory.resolve("wal-load.db");
        Files.deleteIfExists(databasePath);
        Files.deleteIfExists(Path.of(databasePath + "-shm"));
        Files.deleteIfExists(Path.of(databasePath + "-wal"));
        Files.deleteIfExists(walPath);
        Files.deleteIfExists(Path.of(walPath + "-shm"));
        Files.deleteIfExists(Path.of(walPath + "-wal"));

        RevisionManifest fixture = fixture(BLOCK_COUNT);
        String fixtureHash = NarrativeCanonicalMapper.toCanonical(fixture).contentHash();
        DeterministicRenderer renderer = new DeterministicRenderer();
        AdjacentMetadataDetector detector = new AdjacentMetadataDetector();

        for (int warmup = 0; warmup < 5; warmup++) {
            renderer.render(fixture, fixtureHash, RenderRange.all());
        }
        long renderP95 = measureP95Micros(30, () ->
                renderer.render(fixture, fixtureHash, RenderRange.all())
        );

        for (int warmup = 0; warmup < 3; warmup++) {
            detector.detect(fixture, fixtureHash, RenderRange.all());
        }
        long detectorP95 = measureP95Micros(20, () ->
                detector.detect(fixture, fixtureHash, RenderRange.all())
        );

        byte[] firstRender = CanonicalJson.bytes(renderer.render(
                fixture, fixtureHash, RenderRange.all()
        ).canonicalValue());
        boolean deterministic = true;
        for (int repetition = 0; repetition < 10; repetition++) {
            byte[] repeated = CanonicalJson.bytes(renderer.render(
                    fixture, fixtureHash, RenderRange.all()
            ).canonicalValue());
            deterministic &= Arrays.equals(firstRender, repeated);
        }

        long commitTransactionP95;
        long commitTotalP95;
        try (SqliteRevisionStore store = SqliteRevisionStore.open(databasePath)) {
            store.createNovel(fixture, fixtureHash);
            List<Long> transactionSamples = new ArrayList<>();
            for (int index = 0; index < 20; index++) {
                RevisionManifest base = head(store, fixture.novel().id());
                EditOperation operation = metadataOperation(base, "transaction-" + index);
                Ids.RevisionId candidateId = Ids.RevisionId.create();
                Instant at = fixture.createdAt().plusSeconds(index + 1L);
                RevisionManifest candidate = new NarrativeEditor(revisionId ->
                        store.getRevision(base.novel().id(), revisionId).manifest()
                ).apply(base, operation, candidateId, at);
                String candidateHash = NarrativeCanonicalMapper.toCanonical(
                        candidate
                ).contentHash();
                CommitRequest request = new CommitRequest(
                        store.getHead(base.novel().id()),
                        operation,
                        EditOperationCanonicalMapper.hash(operation),
                        candidate,
                        candidateHash
                );
                long started = System.nanoTime();
                store.commitCas(
                        request,
                        AuditContext.system("slo-transaction-" + index, at)
                );
                transactionSamples.add(elapsedMicros(started));
            }
            commitTransactionP95 = p95(transactionSamples);

            CommitService service = new CommitService(store);
            List<Long> totalSamples = new ArrayList<>();
            for (int index = 0; index < 20; index++) {
                RevisionManifest base = head(store, fixture.novel().id());
                EditOperation operation = metadataOperation(base, "total-" + index);
                Instant at = fixture.createdAt().plusSeconds(100L + index);
                long started = System.nanoTime();
                service.commit(operation, Ids.RevisionId.create(), at);
                totalSamples.add(elapsedMicros(started));
            }
            commitTotalP95 = p95(totalSamples);
        }

        SqliteWalSpike.Report load = SqliteWalSpike.runMultiProcess(
                walPath, 2, 2, 50, 200
        );
        long throughputMilliPerSecond = load.elapsedMillis() == 0
                ? Long.MAX_VALUE
                : load.writes() * 1_000_000L / load.elapsedMillis();
        boolean writesPass = load.writes() == 100
                && load.finalRows() == 100
                && throughputMilliPerSecond >= 2_000;

        Map<String, Object> results = new LinkedHashMap<>();
        results.put(NFR_001, latencyResult(
                "1000-block range render p95", renderP95, 500_000
        ));
        results.put(NFR_002, latencyResult(
                "1000-block deterministic detector p95", detectorP95, 1_000_000
        ));
        results.put(NFR_003, latencyResult(
                "short SQLite commit transaction p95", commitTransactionP95, 100_000
        ));
        results.put(NFR_004, latencyResult(
                "commit service total p95 excluding network and LLM",
                commitTotalP95,
                250_000
        ));
        results.put(NFR_005, Map.of(
                "busy_total", load.busyTotal(),
                "elapsed_ms", load.elapsedMillis(),
                "final_rows", load.finalRows(),
                "metric", "multi-process sustained writes",
                "passed", writesPass,
                "target_milli_commits_per_second", 2_000,
                "throughput_milli_commits_per_second", throughputMilliPerSecond,
                "writes", load.writes()
        ));
        results.put(NFR_009, Map.of(
                "comparisons", 10,
                "metric", "same-version render bytes",
                "output_sha256", CanonicalJson.hashBytes(firstRender),
                "passed", deterministic
        ));
        boolean passed = results.values().stream()
                .map(value -> (Map<?, ?>) value)
                .allMatch(value -> Boolean.TRUE.equals(value.get("passed")));
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("block_count", BLOCK_COUNT);
        report.put("database_path", databasePath.toString());
        report.put("hardware", hardware());
        report.put("nfr", results);
        report.put("passed", passed);
        report.put("schema_version", "adr-318-core-slo-1");
        Files.write(outputDirectory.resolve("core.json"), CanonicalJson.bytes(report));

        assertTrue(passed, "Core SLO failure is recorded in target/slo/core.json");
    }

    private static Map<String, Object> latencyResult(
            String metric,
            long measuredMicros,
            long targetMicros
    ) {
        return Map.of(
                "measured_p95_us", measuredMicros,
                "metric", metric,
                "passed", measuredMicros < targetMicros,
                "samples", metric.startsWith("1000-block deterministic") ? 20 :
                        metric.startsWith("1000-block range") ? 30 : 20,
                "target_p95_us", targetMicros
        );
    }

    private static Map<String, Object> hardware() {
        Runtime runtime = Runtime.getRuntime();
        return Map.of(
                "available_processors", runtime.availableProcessors(),
                "java_runtime", System.getProperty("java.runtime.version"),
                "jvm_max_heap_bytes", runtime.maxMemory(),
                "os_arch", System.getProperty("os.arch"),
                "os_name", System.getProperty("os.name"),
                "os_version", System.getProperty("os.version")
        );
    }

    private static long measureP95Micros(int samples, CheckedRunnable operation)
            throws Exception {
        List<Long> values = new ArrayList<>();
        for (int sample = 0; sample < samples; sample++) {
            long started = System.nanoTime();
            operation.run();
            values.add(elapsedMicros(started));
        }
        return p95(values);
    }

    private static long elapsedMicros(long started) {
        return TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - started);
    }

    private static long p95(List<Long> samples) {
        List<Long> sorted = samples.stream().sorted().toList();
        int index = (int) StrictMath.ceil(sorted.size() * 0.95d) - 1;
        return sorted.get(Math.max(0, index));
    }

    private static RevisionManifest head(
            SqliteRevisionStore store,
            Ids.NovelId novelId
    ) {
        RevisionRef head = store.getHead(novelId);
        return store.getRevision(novelId, head.revisionId()).manifest();
    }

    private static EditOperation metadataOperation(
            RevisionManifest base,
            String idempotencyKey
    ) {
        NarrativeScene scene = base.novel().chapters().getFirst().scenes().getFirst();
        NarrativeBlock block = scene.blocks().getFirst();
        BlockMetadata next = block.metadata().fields().isEmpty()
                ? new BlockMetadata(Map.of("narrative_mode", "action"))
                : BlockMetadata.empty();
        String hash = NarrativeCanonicalMapper.toCanonical(base).contentHash();
        return new EditOperation.CorrectBlockMeta(
                new EditContext(
                        Ids.OperationId.create(),
                        idempotencyKey,
                        base.novel().id(),
                        base.id(),
                        hash
                ),
                scene.id(),
                BlockVersionRef.from(block),
                next
        );
    }

    private static RevisionManifest fixture(int blockCount) {
        Ids.ChapterId chapterId = Ids.ChapterId.create();
        List<NarrativeBlock> blocks = new ArrayList<>();
        for (int index = 0; index < blockCount; index++) {
            blocks.add(NarrativeBlock.create(
                    Ids.BlockId.create(),
                    OrderKey.rebalanced(index, blockCount),
                    "Block %04d remains stable.".formatted(index),
                    BlockMetadata.empty(),
                    Map.of()
            ));
        }
        NarrativeScene scene = new NarrativeScene(
                Ids.SceneId.create(),
                chapterId,
                OrderKey.initial(),
                "SLO scene",
                TransitionMode.OPENING,
                SceneSeed.empty(),
                blocks,
                Map.of()
        );
        NarrativeChapter chapter = new NarrativeChapter(
                chapterId,
                OrderKey.initial(),
                "SLO chapter",
                List.of(scene),
                Map.of()
        );
        return new RevisionManifest(
                Ids.RevisionId.create(),
                null,
                Instant.parse("2026-08-23T00:00:00Z"),
                new NarrativeNovel(Ids.NovelId.create(), List.of(chapter), Map.of())
        );
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
