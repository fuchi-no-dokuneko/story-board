package dev.storyblock.worker.style;

import dev.storyblock.style.StyleAnalysisExecutor;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

final class StyleWorkerClient {
    static final int MAX_CLAIM_RESPONSE_BYTES = 16 * 1024 * 1024;
    static final int MAX_RESULT_REQUEST_BYTES = 2 * 1024 * 1024;
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    final HttpClient http;
    final StyleWorkerSettings settings;
    final Clock clock;
    final StyleAnalysisExecutor executor;
    final Supplier<String> idempotencyKeys;

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
        return StyleWorkerClientRunOnceAction.runOnce(this);
    }

    HttpResponse<byte[]> send(HttpRequest request)
            throws IOException, InterruptedException {
        return StyleWorkerClientSendAction.send(this, request);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parseObject(byte[] body, String path) {
        return StyleWorkerClientParseObjectFactory.parseObject(body, path);
    }

    String nextKey() {
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
