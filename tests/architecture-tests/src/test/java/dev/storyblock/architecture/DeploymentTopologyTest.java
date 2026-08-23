package dev.storyblock.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DeploymentTopologyTest {
    private static final Path ROOT = Path.of("../..").toAbsolutePath().normalize();

    @Test
    void productionTopologyKeepsSecretsAndDatabasePrivate() throws Exception {
        String compose = Files.readString(ROOT.resolve("compose.yaml"));
        String dockerfile = Files.readString(ROOT.resolve("Dockerfile"));
        String proxy = Files.readString(ROOT.resolve("deploy/Caddyfile"));

        String api = section(compose, "\n  api:", "\n  style-worker:");
        String styleWorker = section(
                compose, "\n  style-worker:", "\n  llm-worker:"
        );
        String llmWorker = section(compose, "\n  llm-worker:", "\nsecrets:");

        assertTrue(api.contains("storyblock-data:/app/data"));
        assertFalse(styleWorker.contains("storyblock-data"));
        assertFalse(llmWorker.contains("storyblock-data"));
        assertFalse(api.contains("ports:"));
        assertTrue(compose.contains("internal:\n    internal: true"));
        assertTrue(compose.contains("/run/secrets/owner-token"));
        assertTrue(compose.contains("/run/secrets/server-pepper"));
        assertFalse(compose.contains("STORYBLOCK_SECURITY_OWNER_TOKEN: ${"));
        assertTrue(dockerfile.contains("USER storyblock"));
        assertTrue(proxy.contains("@public path /v1 /v1/* /actuator/health"));
    }

    private static String section(String compose, String marker, String nextMarker) {
        int start = compose.indexOf(marker);
        int end = compose.indexOf(nextMarker, start + 1);
        return compose.substring(start, end);
    }
}
