package dev.storyblock.worker.style;

import static org.junit.jupiter.api.Assertions.*;
import dev.storyblock.domain.Ids;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ReachableWorkerTest {
    @Test
    void defaultWorkerClaimsOverSelfSignedHttpsWithoutCredentials() throws Exception {
        var server = HttpsTestServer.create();
        var authorization = new AtomicReference<String>("unobserved");
        server.createContext("/v1/internal/jobs/claims", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            var settings = new StyleWorkerSettings(
                    URI.create("https://127.0.0.1:" + server.getAddress().getPort()),
                    "", Ids.NovelId.create(), "local-worker", Duration.ofMinutes(5),
                    Duration.ofSeconds(1), true);
            var http = HttpClient.newBuilder().sslContext(LocalSelfSignedTls.context()).build();
            assertEquals(StyleWorkerClient.Outcome.NO_JOB,
                    new StyleWorkerClient(http, settings, Clock.systemUTC()).runOnce());
            assertNull(authorization.get());
        } finally { server.stop(0); }
    }
}
