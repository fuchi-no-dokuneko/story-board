package dev.storyblock.storage.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public final class SqliteWalSpike {
    static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(45);

    private SqliteWalSpike() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length > 0 && arguments[0].equals("--worker")) {
            SqliteWalSpikeRunWorker.runWorker(arguments);
            return;
        }
        if (arguments.length < 1 || arguments.length > 2) {
            throw new IllegalArgumentException(
                    "Usage: SqliteWalSpike <disposable-db-path> [writes-per-process]"
            );
        }
        int writesPerProcess = arguments.length == 2 ? Integer.parseInt(arguments[1]) : 50;
        Report report = runMultiProcess(Path.of(arguments[0]), 2, 2, writesPerProcess, 200);
        System.out.println(report.toJson());
    }

    public static Report runMultiProcess(
            Path databasePath,
            int writerProcesses,
            int readerProcesses,
            int writesPerProcess,
            int readsPerProcess
    ) throws Exception {
        SqliteWalSpikeRequirePositive.requirePositive(writerProcesses, "writerProcesses");
        SqliteWalSpikeRequirePositive.requirePositive(readerProcesses, "readerProcesses");
        SqliteWalSpikeRequirePositive.requirePositive(writesPerProcess, "writesPerProcess");
        SqliteWalSpikeRequirePositive.requirePositive(readsPerProcess, "readsPerProcess");
        Path absoluteDatabase = databasePath.toAbsolutePath().normalize();
        SqliteWalSpikeInitialize.initialize(absoluteDatabase);

        Path workerDirectory = Files.createTempDirectory("storyblock-wal-workers-");
        List<WorkerProcess> workers = new ArrayList<>();
        long synchronizedStart = System.currentTimeMillis() + 1_000L;
        long started = System.nanoTime();
        try {
            for (int index = 0; index < writerProcesses; index++) {
                workers.add(SqliteWalSpikeStartWorker.startWorker(
                        "writer-" + index,
                        "writer",
                        absoluteDatabase,
                        writesPerProcess,
                        synchronizedStart,
                        workerDirectory
                ));
            }
            for (int index = 0; index < readerProcesses; index++) {
                workers.add(SqliteWalSpikeStartWorker.startWorker(
                        "reader-" + index,
                        "reader",
                        absoluteDatabase,
                        readsPerProcess,
                        synchronizedStart,
                        workerDirectory
                ));
            }

            List<Properties> results = new ArrayList<>();
            for (WorkerProcess worker : workers) {
                SqliteWalSpikeAwaitWorker.awaitWorker(worker);
                results.add(SqliteWalSpikeLoad.load(worker.resultFile()));
            }

            long writes = SqliteWalSpikeSum.sum(results, "writes");
            long reads = SqliteWalSpikeSum.sum(results, "reads");
            long busy = SqliteWalSpikeSum.sum(results, "busy_total");
            long connectionVerifications = SqliteWalSpikeSum.sum(results, "connection_verifications");
            long writerWaitMillis = SqliteWalSpikeSum.sum(results, "writer_wait_ms");
            long maxTransactionMillis = SqliteWalSpikeMax.max(results, "max_transaction_ms");
            long maxObservedRows = SqliteWalSpikeMax.max(results, "max_observed_rows");
            long expectedRows = (long) writerProcesses * writesPerProcess;

            try (SqliteDatabase database = SqliteDatabase.open(absoluteDatabase)) {
                long finalRows = SqliteWalSpikeCountRows.countRows(database);
                SqliteWalCheckpoint checkpoint = database.checkpointPassive();
                if (writes != expectedRows || finalRows != expectedRows) {
                    throw new IllegalStateException(
                            "WAL spike lost writes: expected=" + expectedRows
                                    + ", worker-reported=" + writes
                                    + ", stored=" + finalRows
                    );
                }
                return new Report(
                        writerProcesses,
                        readerProcesses,
                        writes,
                        reads,
                        finalRows,
                        busy,
                        connectionVerifications,
                        writerWaitMillis,
                        maxTransactionMillis,
                        maxObservedRows,
                        checkpoint,
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                );
            }
        } finally {
            SqliteWalSpikeStopRemainingWorkers.stopRemainingWorkers(workers);
            SqliteWalSpikeCleanupWorkerFiles.cleanupWorkerFiles(workers, workerDirectory);
        }
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
