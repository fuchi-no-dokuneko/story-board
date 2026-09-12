package dev.storyblock.storage.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static dev.storyblock.storage.sqlite.SqliteWalSpike.WorkerProcess;

final class SqliteWalSpikeCleanupWorkerFiles {
    static void cleanupWorkerFiles(List<WorkerProcess> workers, Path directory) {
        try {
            for (WorkerProcess worker : workers) {
                Files.deleteIfExists(worker.resultFile());
                Files.deleteIfExists(worker.outputFile());
            }
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // Temporary diagnostics are safe to retain when cleanup is unavailable.
        }
    }
}
