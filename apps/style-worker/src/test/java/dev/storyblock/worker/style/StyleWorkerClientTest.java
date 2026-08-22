package dev.storyblock.worker.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import dev.storyblock.style.StyleAnalysisBlock;
import dev.storyblock.style.StyleAnalysisCompletionCommand;
import dev.storyblock.style.StyleAnalysisLease;
import dev.storyblock.style.StyleAnalysisSnapshot;
import dev.storyblock.style.StyleAnalysisSummary;
import dev.storyblock.style.StyleAnalysisTrace;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleCalibrationProfile;
import dev.storyblock.style.StyleChannelCalibration;
import dev.storyblock.style.StyleCorpusSource;
import dev.storyblock.style.StyleCorpusSourceKind;
import dev.storyblock.style.StyleFeatureAnalyzer;
import dev.storyblock.style.StyleFeatureChannel;
import dev.storyblock.style.StyleFeatureContract;
import dev.storyblock.style.StyleMaskingLexicon;
import dev.storyblock.style.StyleModule;
import dev.storyblock.style.StyleProfileScope;
import dev.storyblock.style.StyleProfileVersion;
import dev.storyblock.style.StyleProfileVersionContent;
import dev.storyblock.style.StyleScopeKind;
import dev.storyblock.style.StyleStratum;
import dev.storyblock.style.StyleStratumCalibration;
import dev.storyblock.style.StyleStratumKind;
import dev.storyblock.style.StyleWindowConfiguration;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class StyleWorkerClientTest {
    private static final Instant NOW = Instant.parse("2026-08-21T16:00:00Z");
    private static final String TOKEN = "nv_key_00000000-0000-0000-0000-000000000001."
            + "A".repeat(43);

    @Test
    void claimsExecutesAndSubmitsHashVerifiedCompressedResult() throws Exception {
        StyleAnalysisLease lease = lease();
        AtomicReference<Throwable> handlerFailure = new AtomicReference<>();
        AtomicReference<Map<String, Object>> submittedResult = new AtomicReference<>();
        AtomicReference<String> claimKey = new AtomicReference<>();
        AtomicInteger claimRequests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByName("127.0.0.1"), 0
        ), 0);
        server.createContext("/v1/internal/jobs/claims", exchange -> handle(
                exchange,
                handlerFailure,
                () -> {
                    assertEquals("POST", exchange.getRequestMethod());
                    assertEquals(
                            "Bearer " + TOKEN,
                            exchange.getRequestHeaders().getFirst("Authorization")
                    );
                    assertEquals("*", exchange.getRequestHeaders().getFirst("If-Match"));
                    Map<String, Object> claim = object(
                            exchange.getRequestBody().readAllBytes()
                    );
                    assertEquals(lease.snapshot().novelId().value(), claim.get("novel_id"));
                    assertEquals(lease.leaseOwner(), claim.get("lease_owner"));
                    String currentKey = exchange.getRequestHeaders().getFirst(
                            "Idempotency-Key"
                    );
                    if (claimKey.get() == null) {
                        claimKey.set(currentKey);
                    }
                    assertEquals(claimKey.get(), currentKey);
                    if (claimRequests.incrementAndGet() == 1) {
                        respond(exchange, 503, CanonicalJson.bytes(Map.of(
                                "code", "DEPENDENCY_UNAVAILABLE"
                        )));
                        return;
                    }
                    exchange.getResponseHeaders().add(
                            "ETag", quote(lease.claimedStatusHash())
                    );
                    respond(exchange, 200, CanonicalJson.bytes(lease.canonicalValue()));
                }
        ));
        server.createContext(
                "/v1/internal/jobs/" + lease.jobId().value() + "/results",
                exchange -> handle(exchange, handlerFailure, () -> {
                    assertEquals(
                            quote(lease.claimedStatusHash()),
                            exchange.getRequestHeaders().getFirst("If-Match")
                    );
                    Map<String, Object> body = object(
                            exchange.getRequestBody().readAllBytes()
                    );
                    submittedResult.set(body);
                    StyleAnalysisCompletionCommand completion = completion(lease, body);
                    respond(exchange, 200, CanonicalJson.bytes(Map.of(
                            "analysis_id", lease.analysisId().value(),
                            "idempotent_replay", false,
                            "job_id", lease.jobId().value(),
                            "result_hash", completion.resultHash(),
                            "status", "succeeded"
                    )));
                })
        );
        server.start();

        try {
            StyleWorkerSettings settings = new StyleWorkerSettings(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                    TOKEN,
                    lease.snapshot().novelId(),
                    lease.leaseOwner(),
                    Duration.ofMinutes(5),
                    Duration.ofSeconds(1),
                    true
            );
            StyleWorkerClient client = new StyleWorkerClient(
                    HttpClient.newHttpClient(),
                    settings,
                    Clock.fixed(NOW, ZoneOffset.UTC),
                    new dev.storyblock.style.StyleAnalysisExecutor(),
                    () -> "fixed-claim-key"
            );

            assertEquals(StyleWorkerClient.Outcome.COMPLETED, client.runOnce());
            assertEquals(2, claimRequests.get());
            assertNull(handlerFailure.get());
            Map<String, Object> trace = map(submittedResult.get().get("trace"));
            assertEquals("gzip", trace.get("codec"));
            assertTrue(((String) trace.get("content_base64")).length() > 2);
            assertFalse(settings.toString().contains(TOKEN));
            assertTrue(settings.toString().contains("<redacted>"));
        } finally {
            server.stop(0);
        }
    }

    private static StyleAnalysisCompletionCommand completion(
            StyleAnalysisLease lease,
            Map<String, Object> body
    ) {
        Map<String, Object> traceEnvelope = map(body.get("trace"));
        Instant completedAt = Instant.parse((String) body.get("completed_at"));
        StyleAnalysisTrace trace = StyleAnalysisTrace.fromCompressed(
                lease.analysisId(),
                (String) traceEnvelope.get("content_hash"),
                Base64.getDecoder().decode(
                        (String) traceEnvelope.get("content_base64")
                ),
                ((Number) traceEnvelope.get("uncompressed_bytes")).intValue(),
                completedAt,
                lease.retentionUntil()
        );
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> windows = (List<Map<String, Object>>) body.get(
                "windows"
        );
        return new StyleAnalysisCompletionCommand(
                lease.jobId(),
                (String) body.get("lease_owner"),
                ((Number) body.get("attempt")).intValue(),
                lease.claimedStatusHash(),
                (String) body.get("snapshot_hash"),
                (String) body.get("profile_version_hash"),
                (String) body.get("analyzer_contract_hash"),
                (String) body.get("window_configuration_hash"),
                StyleAnalysisSummary.fromCanonical(map(body.get("summary"))),
                windows.stream().map(StyleAnalysisWindowFinding::fromCanonical).toList(),
                trace,
                "style-result-" + lease.jobId().value() + "-" + lease.attempt(),
                completedAt
        );
    }

    private static StyleAnalysisLease lease() {
        Ids.NovelId novelId = Ids.NovelId.create();
        Ids.SceneId sceneId = Ids.SceneId.create();
        List<NarrativeBlock> narrativeBlocks = new ArrayList<>();
        List<StyleAnalysisBlock> analysisBlocks = new ArrayList<>();
        for (int index = 0; index < 60; index++) {
            NarrativeBlock block = NarrativeBlock.create(
                    Ids.BlockId.create(),
                    OrderKey.rebalanced(index, 60),
                    "Measured narrative sentence %02d repeats a stable cadence."
                            .formatted(index),
                    BlockMetadata.empty(),
                    Map.of()
            );
            narrativeBlocks.add(block);
            analysisBlocks.add(new StyleAnalysisBlock(
                    sceneId,
                    block,
                    StyleStratumKind.NARRATION,
                    null,
                    "unknown",
                    "narration",
                    null
            ));
        }
        StyleMaskingLexicon lexicon = StyleMaskingLexicon.empty();
        var features = new StyleFeatureAnalyzer().extract(
                narrativeBlocks,
                lexicon,
                StyleFeatureContract.defaults(lexicon.vocabularyHash())
        );
        StyleWindowConfiguration windows = StyleWindowConfiguration.defaults();
        List<BigDecimal> distances = java.util.Collections.nCopies(
                30, new BigDecimal("0.1")
        );
        StyleCalibrationProfile calibration = new StyleCalibrationProfile(
                StyleModule.CALIBRATION_SCHEMA_VERSION,
                features.sourceHash(),
                features.contract().contractHash(),
                windows.configurationHash(),
                List.of(new StyleStratumCalibration(
                        StyleStratum.narration(),
                        30,
                        StyleFeatureChannel.requiredChannels().stream()
                                .sorted(java.util.Comparator.comparing(Enum::ordinal))
                                .map(channel -> StyleChannelCalibration.fromDistances(
                                        channel, distances
                                )).toList()
                ))
        );
        StyleProfileScope scope = new StyleProfileScope(
                novelId, StyleScopeKind.NOVEL, null
        );
        StyleProfileVersionContent content = new StyleProfileVersionContent(
                scope,
                List.of(new StyleCorpusSource(
                        "worker-test-corpus",
                        features.sourceHash(),
                        StyleCorpusSourceKind.OWNER,
                        "Worker protocol test corpus",
                        "test-only",
                        "test-owner"
                )),
                features,
                windows,
                calibration.canonicalValue()
        );
        StyleProfileVersion version = new StyleProfileVersion(
                Ids.StyleProfileVersionId.create(),
                Ids.StyleProfileId.create(),
                1,
                content,
                "test-owner",
                NOW.minus(Duration.ofDays(1))
        );
        StyleAnalysisSnapshot snapshot = new StyleAnalysisSnapshot(
                novelId,
                Ids.RevisionId.create(),
                CanonicalJson.hash("worker-test-revision"),
                version,
                lexicon,
                analysisBlocks
        );
        return new StyleAnalysisLease(
                Ids.JobId.create(),
                Ids.StyleAnalysisId.create(),
                snapshot,
                "worker-test",
                1,
                NOW.plus(Duration.ofMinutes(5)),
                NOW.plus(Duration.ofDays(1)),
                CanonicalJson.hash("worker-test-running-status"),
                false
        );
    }

    private static void handle(
            HttpExchange exchange,
            AtomicReference<Throwable> failure,
            ExchangeAction action
    ) throws IOException {
        try {
            action.run();
        } catch (Throwable thrown) {
            failure.compareAndSet(null, thrown);
            respond(exchange, 500, new byte[0]);
        }
    }

    private static void respond(HttpExchange exchange, int status, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(byte[] body) {
        return CanonicalJson.mapper().readValue(body, Map.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private static String quote(String value) {
        return '"' + value + '"';
    }

    @FunctionalInterface
    private interface ExchangeAction {
        void run() throws Exception;
    }
}
