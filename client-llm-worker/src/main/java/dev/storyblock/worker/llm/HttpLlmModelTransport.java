package dev.storyblock.worker.llm;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

final class HttpLlmModelTransport implements LlmModelTransport {
    private final HttpClient client;
    private final LlmWorkerSettings settings;
    private final byte[] credentialBytes;
    private final int[] credentialPrefix;

    HttpLlmModelTransport(HttpClient client, LlmWorkerSettings settings) {
        this.client = Objects.requireNonNull(client, "client");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.credentialBytes = settings.modelToken().getBytes(StandardCharsets.UTF_8);
        this.credentialPrefix = prefixTable(credentialBytes);
    }

    @Override
    public byte[] invoke(byte[] canonicalRequest)
            throws IOException, InterruptedException {
        Objects.requireNonNull(canonicalRequest, "canonicalRequest");
        if (contains(canonicalRequest, credentialBytes, credentialPrefix)) {
            throw new LlmWorkerProtocolException(
                    "Model request contains a transport credential"
            );
        }
        HttpRequest request = HttpRequest.newBuilder(settings.modelEndpoint())
                .timeout(settings.requestTimeout())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + settings.modelToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(canonicalRequest))
                .build();
        HttpResponse<InputStream> response = client.send(
                request, HttpResponse.BodyHandlers.ofInputStream()
        );
        try (InputStream body = response.body()) {
            if (response.statusCode() != 200) {
                throw new LlmWorkerProtocolException(
                        "Model endpoint returned HTTP " + response.statusCode()
                );
            }
            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("")
                    .toLowerCase(Locale.ROOT);
            if (!(contentType.equals("application/json")
                    || contentType.startsWith("application/json;"))) {
                throw new LlmWorkerProtocolException(
                        "Model endpoint response is not JSON"
                );
            }
            long declaredLength = response.headers().firstValueAsLong(
                    "Content-Length"
            ).orElse(-1L);
            if (declaredLength > settings.maxResponseBytes()) {
                throw new LlmWorkerProtocolException(
                        "Model endpoint response exceeds the byte limit"
                );
            }
            byte[] result = body.readNBytes(settings.maxResponseBytes() + 1);
            if (result.length > settings.maxResponseBytes()) {
                throw new LlmWorkerProtocolException(
                        "Model endpoint response exceeds the byte limit"
                );
            }
            if (contains(result, credentialBytes, credentialPrefix)) {
                throw new LlmWorkerProtocolException(
                        "Model response contains a transport credential"
                );
            }
            return result;
        }
    }

    private static boolean contains(
            byte[] value,
            byte[] candidate,
            int[] prefix
    ) {
        if (candidate.length == 0 || candidate.length > value.length) {
            return false;
        }
        int matched = 0;
        for (byte current : value) {
            while (matched > 0 && current != candidate[matched]) {
                matched = prefix[matched - 1];
            }
            if (current == candidate[matched]) {
                matched++;
            }
            if (matched == candidate.length) {
                return true;
            }
        }
        return false;
    }

    private static int[] prefixTable(byte[] candidate) {
        int[] prefix = new int[candidate.length];
        int matched = 0;
        for (int index = 1; index < candidate.length; index++) {
            while (matched > 0 && candidate[index] != candidate[matched]) {
                matched = prefix[matched - 1];
            }
            if (candidate[index] == candidate[matched]) {
                matched++;
            }
            prefix[index] = matched;
        }
        return prefix;
    }
}
