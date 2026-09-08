package dev.storyblock.storage.sqlite;

import java.util.List;
import static dev.storyblock.storage.sqlite.SqliteWalSpike.WorkerProcess;

final class SqliteWalSpikeStopRemainingWorkers {
    static void stopRemainingWorkers(List<WorkerProcess> workers) {
        for (WorkerProcess worker : workers) {
            if (worker.process().isAlive()) {
                worker.process().destroyForcibly();
            }
        }
    }
}
