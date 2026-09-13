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
        String entrypoint = Files.readString(
                ROOT.resolve("scripts/container-entrypoint.sh")
        );
        String tlsGenerator = Files.readString(
                ROOT.resolve("server/src/main/java/dev/storyblock/api/runtime/LocalTlsMaterial.java")
        );

        String api = section(compose, "\n  api:", "\n  style-worker:");
        String styleWorker = section(
                compose, "\n  style-worker:", "\n  llm-worker:"
        );
        String llmWorker = section(compose, "\n  llm-worker:", "\nnetworks:");

        assertTrue(api.contains("./server/data:/workspace/server/data"));
        assertFalse(styleWorker.contains("/workspace/server/data"));
        assertFalse(llmWorker.contains("/workspace/server/data"));
        assertTrue(api.contains("0.0.0.0"));
        assertTrue(api.contains("8443}:8443"));
        assertTrue(api.contains("STORYBLOCK_TRUSTED_LAN_ENABLED: \"true\""));
        assertFalse(compose.contains("\n  proxy:"));
        assertTrue(compose.contains("internal:\n    internal: true"));
        assertFalse(compose.contains("/run/secrets"));
        assertFalse(compose.contains("STORYBLOCK_SECURITY_OWNER_TOKEN: ${"));
        assertTrue(dockerfile.contains("USER storyblock"));
        assertTrue(dockerfile.contains("EXPOSE 8443"));
        assertTrue(entrypoint.contains("exec java"));
        assertTrue(entrypoint.contains("-Djava.io.tmpdir=$data_dir/tmp"));
        assertTrue(entrypoint.contains("-jar /workspace/application.jar"));
        String tls = Files.readString(ROOT.resolve(
                "server/src/main/java/dev/storyblock/api/runtime/LocalServerTls.java"));
        assertTrue(tls.contains("LocalTlsMaterial"));
        assertTrue(tlsGenerator.contains("BC=ca:false"));
        assertTrue(tlsGenerator.contains("EKU=serverAuth"));
    }

    private static String section(String compose, String marker, String nextMarker) {
        int start = compose.indexOf(marker);
        int end = compose.indexOf(nextMarker, start + 1);
        return compose.substring(start, end);
    }
}
