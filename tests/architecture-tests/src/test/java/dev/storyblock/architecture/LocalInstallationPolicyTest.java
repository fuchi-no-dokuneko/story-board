package dev.storyblock.architecture;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
class LocalInstallationPolicyTest {
    private static final Path ROOT = Path.of("../..").toAbsolutePath().normalize();
    @Test
    void localInstallerIsUnprivilegedRepositoryLocalAndRepeatable() throws Exception {
        String installer = Files.readString(ROOT.resolve("install.sh"))
                + Files.readString(ROOT.resolve("scripts/install-runtime.sh"))
                + Files.readString(ROOT.resolve("scripts/build-environment.sh"))
                + Files.readString(ROOT.resolve("install-local-build.sh"));

        assertTrue(installer.contains("local_dir=$repo_dir/server/data"));
        assertTrue(installer.contains("scripts/generate-self-signed-tls.sh"));
        assertTrue(installer.contains("install -m 500 --"));
        assertTrue(installer.contains("mv -f -- \"$staged_jar\" \"$installed_jar\""));
        assertFalse(installer.matches("(?s).*\\n\\s*sudo\\s+.*"));
        assertFalse(installer.contains("$HOME"));
        assertFalse(installer.contains("/etc/"));
        assertFalse(installer.contains("/opt/"));
        assertFalse(installer.contains("/var/"));
    }

}
