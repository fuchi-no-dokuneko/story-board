package dev.storyblock.worker.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.style.StyleAnalysisCompletionCommand;
import dev.storyblock.style.StyleAnalysisExecution;
import dev.storyblock.style.StyleAnalysisExecutor;
import dev.storyblock.style.StyleAnalysisLease;
import dev.storyblock.style.StyleAnalysisTrace;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

final class StyleWorkerClient {
    private static final int MAX_CLAIM_RESPONSE_BYTES = 16 * 1024 * 1024;
    private static final int MAX_RESULT_REQUEST_BYTES = 2 * 1024 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient http;
    private final StyleWorkerSettings settings;
    private final Clock clock;
    private final StyleAnalysisExecutor executor;
    private final Supplier<String> idempotencyKeys;

    StyleWorkerClient(
            HttpClient http,
            StyleWorkerSettings settings,
            Clock clock
    ) {
        this(
                http,
                settings,
                clock,
                new StyleAnalysisExecutor(),
                () -> UUID.randomUUID().toString()
        );
    }

    StyleWorkerClient(
            HttpClient http,
            StyleWorkerSettings settings,
            Clock clock,
            StyleAnalysisExecutor executor,
            Supplier<String> idempotencyKeys
    ) {
        this.http = Objects.requireNonNull(http, "http");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.idempotencyKeys = Objects.requireNonNull(
                idempotencyKeys, "idempotencyKeys"
        );
    }

    Outcome runOnce() throws IOException, InterruptedException {
        String claimKey = "style-claim-" + nextKey();
        byte[] claimBody = CanonicalJson.bytes(Map.of(
                "novel_id", settings.novelId().value(),
                "lease_owner", settings.workerId(),
                "lease_seconds", settings.leaseDuration().toSeconds()
        ));
        HttpResponse<byte[]> claim = send(HttpRequest.newBuilder(
                        settings.endpoint("v1/internal/jobs/claims")
                )
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + settings.bearerToken())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Idempotency-Key", claimKey)
                .header("If-Match", "*")
                .POST(HttpRequest.BodyPublishers.ofByteArray(claimBody))
                .build());
        if (claim.statusCode() == 204) {
            return Outcome.NO_JOB;
        }
        StyleWorkerClientRequireStatus.requireStatus(claim, 200, "claim");
        if (claim.body().length > MAX_CLAIM_RESPONSE_BYTES) {
            throw new StyleWorkerProtocolException(
                    "Style job claim response exceeds the worker limit"
            );
        }
        StyleAnalysisLease lease = StyleAnalysisLease.fromCanonical(
                parseObject(claim.body(), "style job claim response")
        );
        String responseStatusHash = StyleWorkerClientUnquoteEtag.unquoteEtag(claim.headers()
                .firstValue("ETag")
                .orElseThrow(() -> new StyleWorkerProtocolException(
                        "Style job claim response has no ETag"
                )));
        if (!lease.claimedStatusHash().equals(responseStatusHash)
                || !lease.snapshot().novelId().equals(settings.novelId())
                || !lease.leaseOwner().equals(settings.workerId())) {
            throw new StyleWorkerProtocolException(
                    "Style job claim identity or fencing hash is inconsistent"
            );
        }

        StyleAnalysisExecution execution = executor.execute(lease.snapshot());
        Instant completedAt = Instant.now(clock);
        if (!completedAt.isBefore(lease.leaseUntil())) {
            throw new StyleWorkerProtocolException(
                    "Style job lease expired before execution completed"
            );
        }
        StyleAnalysisTrace trace = StyleAnalysisTrace.create(
                lease.analysisId(),
                execution.tracePayload(),
                completedAt,
                lease.retentionUntil()
        );
        String resultKey = "style-result-" + lease.jobId().value()
                + "-" + lease.attempt();
        StyleAnalysisCompletionCommand completion = new StyleAnalysisCompletionCommand(
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
                resultKey,
                completedAt
        );
        byte[] resultBody = StyleWorkerClientResultBody.resultBody(completion);
        if (resultBody.length > MAX_RESULT_REQUEST_BYTES) {
            throw new StyleWorkerProtocolException(
                    "Style job result exceeds the API request limit"
            );
        }
        HttpResponse<byte[]> result = send(HttpRequest.newBuilder(settings.endpoint(
                        "v1/internal/jobs/" + lease.jobId().value() + "/results"
                ))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + settings.bearerToken())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Idempotency-Key", resultKey)
                .header("If-Match", StyleWorkerClientQuoteEtag.quoteEtag(lease.claimedStatusHash()))
                .POST(HttpRequest.BodyPublishers.ofByteArray(resultBody))
                .build());
        StyleWorkerClientRequireStatus.requireStatus(result, 200, "result submission");
        Map<String, Object> response = parseObject(
                result.body(), "style job result response"
        );
        if (!lease.jobId().value().equals(StyleWorkerClientString.string(response, "job_id"))
                || !"succeeded".equals(StyleWorkerClientString.string(response, "status"))
                || !completion.resultHash().equals(StyleWorkerClientString.string(response, "result_hash"))) {
            throw new StyleWorkerProtocolException(
                    "Style job result response does not match the submitted result"
            );
        }
        Object replay = response.get("idempotent_replay");
        if (!(replay instanceof Boolean idempotentReplay)) {
            throw new StyleWorkerProtocolException(
                    "Style job result response has an invalid replay marker"
            );
        }
        return idempotentReplay ? Outcome.REPLAYED : Outcome.COMPLETED;
    }

    private HttpResponse<byte[]> send(HttpRequest request)
            throws IOException, InterruptedException {
        IOException firstFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HttpResponse<byte[]> response = http.send(
                        request, HttpResponse.BodyHandlers.ofByteArray()
                );
                if (attempt == 0 && response.statusCode() >= 500) {
                    continue;
                }
                return response;
            } catch (IOException failure) {
                if (attempt == 1) {
                    if (firstFailure != null) {
                        failure.addSuppressed(firstFailure);
                    }
                    throw failure;
                }
                firstFailure = failure;
            }
        }
        throw new IOException("Style worker request retry did not produce a response");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parseObject(byte[] body, String path) {
        try {
            Object parsed = CanonicalJson.mapper().readValue(body, Map.class);
            if (!(parsed instanceof Map<?, ?> map)
                    || map.keySet().stream().anyMatch(key -> !(key instanceof String))) {
                throw new StyleWorkerProtocolException(path + " must be a JSON object");
            }
            return (Map<String, Object>) map;
        } catch (StyleWorkerProtocolException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new StyleWorkerProtocolException(path + " is malformed", failure);
        }
    }

    private String nextKey() {
        String value = idempotencyKeys.get();
        if (value == null || value.isBlank() || value.length() > 160) {
            throw new StyleWorkerProtocolException(
                    "Style worker idempotency key source returned an invalid value"
            );
        }
        return value;
    }

    enum Outcome {
        NO_JOB,
        COMPLETED,
        REPLAYED
    }
}
