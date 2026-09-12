package dev.storyblock.worker.llm;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

final class HttpLlmModelTransport implements LlmModelTransport {
    final HttpClient client;
    final LlmWorkerSettings settings;
    final byte[] credentialBytes;
    final int[] credentialPrefix;

    HttpLlmModelTransport(HttpClient client, LlmWorkerSettings settings) {
        this.client = Objects.requireNonNull(client, "client");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.credentialBytes = settings.modelToken().getBytes(StandardCharsets.UTF_8);
        this.credentialPrefix = HttpLlmModelTransportPrefixTable.prefixTable(credentialBytes);
    }

    @Override
    public byte[] invoke(byte[] canonicalRequest)
            throws IOException, InterruptedException {
        return HttpLlmModelTransportInvokeAction.invoke(this, canonicalRequest);
    }

}
