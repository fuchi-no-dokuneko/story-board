package dev.storyblock.storage.sqlite;

import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import static dev.storyblock.storage.sqlite.SqliteWalSpike.WorkerProcess;
import static dev.storyblock.storage.sqlite.SqliteWalSpike.PROCESS_TIMEOUT;

final class SqliteWalSpikeAwaitWorker {
    static void awaitWorker(WorkerProcess worker) throws Exception {
        if (!worker.process().waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            worker.process().destroyForcibly();
            throw new IllegalStateException("Timed out waiting for " + worker.workerId());
        }
        if (worker.process().exitValue() != 0) {
            String output = Files.exists(worker.outputFile())
                    ? Files.readString(worker.outputFile())
                    : "<no worker output>";
            throw new IllegalStateException(
                    worker.workerId() + " exited " + worker.process().exitValue() + ": " + output
            );
        }
        if (!Files.isRegularFile(worker.resultFile())) {
            throw new IllegalStateException(worker.workerId() + " produced no structured result");
        }
    }
}
