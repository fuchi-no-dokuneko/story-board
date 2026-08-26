package dev.storyblock.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteProcessCrashTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void processCrashAtEveryCommitBoundaryLeavesOnlyGenesis() throws Exception {
        for (CommitStage stage : CommitStage.values()) {
            Path database = temporaryDirectory.resolve("crash-" + stage + ".db");
            var genesis = RevisionStoreTestFixture.genesis();
            try (SqliteRevisionStore store = SqliteRevisionStore.open(database)) {
                store.createNovel(genesis, RevisionStoreTestFixture.hash(genesis));
            }

            Process child = new ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-Djava.net.preferIPv4Stack=true",
                    "-cp",
                    System.getProperty("java.class.path"),
                    CommitCrashProbe.class.getName(),
                    database.toString(),
                    stage.name()
            ).redirectErrorStream(true).redirectOutput(
                    temporaryDirectory.resolve("crash-" + stage + ".log").toFile()
            ).start();
            assertTrue(child.waitFor(10, TimeUnit.SECONDS), stage.name());
            assertEquals(23, child.exitValue(), stage.name());

            try (SqliteRevisionStore store = SqliteRevisionStore.open(database)) {
                assertEquals(1, store.revisionCount(genesis.novel().id()), stage.name());
                assertEquals(0, store.operationCount(genesis.novel().id()), stage.name());
                assertEquals(genesis.id(), store.getHead(genesis.novel().id()).revisionId());
            }
        }
    }
}
