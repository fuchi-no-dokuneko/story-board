package dev.storyblock.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StoryBlockCliTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void replayVerifyEmitsMachineReadableSuccessReport() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Path database = temporaryDirectory.resolve("empty.db");
        try (SqliteRevisionStore ignored = SqliteRevisionStore.open(database)) {
            // An initialized store with no novels is a valid verification target.
        }

        int status = StoryBlockCli.run(
                new String[]{
                        "replay-verify",
                        database.toString()
                },
                new PrintStream(bytes, true, StandardCharsets.UTF_8),
                System.err
        );

        String output = bytes.toString(StandardCharsets.UTF_8);
        assertEquals(0, status);
        assertTrue(output.contains("\"valid\":true"));
        assertTrue(output.contains("\"novel_count\":0"));
        assertTrue(output.contains("\"render_hashes\":{}"));
    }

    @Test
    void malformedCommandReturnsUsageErrorWithoutOpeningStorage() {
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int status = StoryBlockCli.run(
                new String[]{"unknown"},
                System.out,
                new PrintStream(error, true, StandardCharsets.UTF_8)
        );

        assertEquals(2, status);
        assertTrue(error.toString(StandardCharsets.UTF_8).contains("Unknown command"));
    }

    @Test
    void missingDatabaseFailsInsteadOfCreatingAnEmptyStore() {
        Path missing = temporaryDirectory.resolve("missing.db");
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int status = StoryBlockCli.run(
                new String[]{"replay-verify", missing.toString()},
                System.out,
                new PrintStream(error, true, StandardCharsets.UTF_8)
        );

        assertEquals(1, status);
        assertTrue(error.toString(StandardCharsets.UTF_8).contains("does not exist"));
        assertTrue(java.nio.file.Files.notExists(missing));
    }
}
