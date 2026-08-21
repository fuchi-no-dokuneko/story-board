package dev.storyblock.storage.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteWalSpikeTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void independentProcessesReadWhileShortWritersCommit() throws Exception {
        SqliteWalSpike.Report report = SqliteWalSpike.runMultiProcess(
                temporaryDirectory.resolve("multiprocess.db"),
                2,
                2,
                20,
                100
        );

        assertEquals(40, report.writes());
        assertEquals(200, report.reads());
        assertEquals(40, report.finalRows());
        assertTrue(report.connectionVerifications() >= 4);
        assertEquals(0, report.checkpoint().busy());
        assertEquals(report.checkpoint().logFrames(), report.checkpoint().checkpointedFrames());
        assertTrue(report.elapsedMillis() < 45_000);
    }
}
