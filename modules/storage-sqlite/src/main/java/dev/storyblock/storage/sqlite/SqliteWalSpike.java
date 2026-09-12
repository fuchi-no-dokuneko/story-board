package dev.storyblock.storage.sqlite;

import java.nio.file.Path;
import java.time.Duration;

public final class SqliteWalSpike {
    static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(45);

    SqliteWalSpike() {
    }

    public static void main(String[] arguments) throws Exception {
        SqliteWalSpikeMainFactory.main(arguments);
    }

    public static Report runMultiProcess(
            Path databasePath,
            int writerProcesses,
            int readerProcesses,
            int writesPerProcess,
            int readsPerProcess
    ) throws Exception {
        return SqliteWalSpikeRunMultiProcessFactory.runMultiProcess(databasePath, writerProcesses, readerProcesses, writesPerProcess, readsPerProcess);
    }

    record WorkerProcess(
            String workerId,
            Process process,
            Path resultFile,
            Path outputFile
    ) {
    }

    public record Report(
            int writerProcesses,
            int readerProcesses,
            long writes,
            long reads,
            long finalRows,
            long busyTotal,
            long connectionVerifications,
            long writerWaitMillis,
            long maxTransactionMillis,
            long maxObservedRows,
            SqliteWalCheckpoint checkpoint,
            long elapsedMillis
    ) {
        public String toJson() {
            return """
                    {"writer_processes":%d,"reader_processes":%d,"writes":%d,"reads":%d,"final_rows":%d,"busy_total":%d,"connection_verifications":%d,"writer_wait_ms":%d,"max_transaction_ms":%d,"max_observed_rows":%d,"checkpoint":{"busy":%d,"log_frames":%d,"checkpointed_frames":%d,"duration_ms":%d},"elapsed_ms":%d}
                    """.formatted(
                    writerProcesses,
                    readerProcesses,
                    writes,
                    reads,
                    finalRows,
                    busyTotal,
                    connectionVerifications,
                    writerWaitMillis,
                    maxTransactionMillis,
                    maxObservedRows,
                    checkpoint.busy(),
                    checkpoint.logFrames(),
                    checkpoint.checkpointedFrames(),
                    checkpoint.durationMillis(),
                    elapsedMillis
            ).strip();
        }
    }
}
