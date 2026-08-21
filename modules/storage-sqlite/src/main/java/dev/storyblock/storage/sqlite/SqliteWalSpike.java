package dev.storyblock.storage.sqlite;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public final class SqliteWalSpike {
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(45);

    private SqliteWalSpike() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length > 0 && arguments[0].equals("--worker")) {
            runWorker(arguments);
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
        requirePositive(writerProcesses, "writerProcesses");
        requirePositive(readerProcesses, "readerProcesses");
        requirePositive(writesPerProcess, "writesPerProcess");
        requirePositive(readsPerProcess, "readsPerProcess");
        Path absoluteDatabase = databasePath.toAbsolutePath().normalize();
        initialize(absoluteDatabase);

        Path workerDirectory = Files.createTempDirectory("storyblock-wal-workers-");
        List<WorkerProcess> workers = new ArrayList<>();
        long synchronizedStart = System.currentTimeMillis() + 1_000L;
        long started = System.nanoTime();
        try {
            for (int index = 0; index < writerProcesses; index++) {
                workers.add(startWorker(
                        "writer-" + index,
                        "writer",
                        absoluteDatabase,
                        writesPerProcess,
                        synchronizedStart,
                        workerDirectory
                ));
            }
            for (int index = 0; index < readerProcesses; index++) {
                workers.add(startWorker(
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
                awaitWorker(worker);
                results.add(load(worker.resultFile()));
            }

            long writes = sum(results, "writes");
            long reads = sum(results, "reads");
            long busy = sum(results, "busy_total");
            long connectionVerifications = sum(results, "connection_verifications");
            long writerWaitMillis = sum(results, "writer_wait_ms");
            long maxTransactionMillis = max(results, "max_transaction_ms");
            long maxObservedRows = max(results, "max_observed_rows");
            long expectedRows = (long) writerProcesses * writesPerProcess;

            try (SqliteDatabase database = SqliteDatabase.open(absoluteDatabase)) {
                long finalRows = countRows(database);
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
            stopRemainingWorkers(workers);
            cleanupWorkerFiles(workers, workerDirectory);
        }
    }

    private static void initialize(Path databasePath) throws IOException, SQLException {
        try (SqliteDatabase database = SqliteDatabase.open(databasePath)) {
            database.write(connection -> {
                try (var statement = connection.createStatement()) {
                    statement.execute("DROP TABLE IF EXISTS storyblock_spike_commits");
                    statement.execute("""
                            CREATE TABLE storyblock_spike_commits (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                worker_id TEXT NOT NULL,
                                committed_at_ms INTEGER NOT NULL
                            )
                            """);
                }
                return null;
            });
        }
    }

    private static WorkerProcess startWorker(
            String workerId,
            String role,
            Path databasePath,
            int operations,
            long startAtMillis,
            Path workerDirectory
    ) throws IOException {
        Path result = workerDirectory.resolve(workerId + ".properties");
        Path output = workerDirectory.resolve(workerId + ".log");
        String javaExecutable = Path.of(
                System.getProperty("java.home"),
                "bin",
                isWindows() ? "java.exe" : "java"
        ).toString();
        List<String> command = List.of(
                javaExecutable,
                "-Djava.net.preferIPv4Stack=true",
                "-cp",
                System.getProperty("java.class.path"),
                SqliteWalSpike.class.getName(),
                "--worker",
                role,
                workerId,
                databasePath.toString(),
                Integer.toString(operations),
                Long.toString(startAtMillis),
                result.toString()
        );
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start();
        return new WorkerProcess(workerId, process, result, output);
    }

    private static void awaitWorker(WorkerProcess worker) throws Exception {
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

    private static void runWorker(String[] arguments) throws Exception {
        if (arguments.length != 7) {
            throw new IllegalArgumentException("Invalid WAL spike worker arguments");
        }
        String role = arguments[1];
        String workerId = arguments[2];
        Path databasePath = Path.of(arguments[3]);
        int operations = Integer.parseInt(arguments[4]);
        long startAtMillis = Long.parseLong(arguments[5]);
        Path resultFile = Path.of(arguments[6]);
        sleepUntil(startAtMillis);

        long completedWrites = 0;
        long completedReads = 0;
        long maxObservedRows = 0;
        try (SqliteDatabase database = SqliteDatabase.open(databasePath)) {
            if (role.equals("writer")) {
                completedWrites = runWriter(database, workerId, operations);
            } else if (role.equals("reader")) {
                maxObservedRows = runReader(database, operations);
                completedReads = operations;
            } else {
                throw new IllegalArgumentException("Unknown worker role " + role);
            }

            SqliteMetrics.Snapshot metrics = database.metrics();
            Properties result = new Properties();
            result.setProperty("role", role);
            result.setProperty("writes", Long.toString(completedWrites));
            result.setProperty("reads", Long.toString(completedReads));
            result.setProperty("max_observed_rows", Long.toString(maxObservedRows));
            result.setProperty("busy_total", Long.toString(metrics.sqliteBusyTotal()));
            result.setProperty(
                    "connection_verifications",
                    Long.toString(metrics.connectionVerifications())
            );
            result.setProperty("writer_wait_ms", Long.toString(metrics.writerWaitMillis()));
            result.setProperty(
                    "max_transaction_ms",
                    Long.toString(metrics.maxTransactionMillis())
            );
            try (OutputStream output = Files.newOutputStream(resultFile)) {
                result.store(output, "StoryBlock SQLite WAL spike worker result");
            }
        }
    }

    private static long runWriter(
            SqliteDatabase database,
            String workerId,
            int writes
    ) throws Exception {
        long completed = 0;
        long deadline = System.nanoTime() + PROCESS_TIMEOUT.toNanos();
        while (completed < writes) {
            try {
                database.write(connection -> {
                    try (var statement = connection.prepareStatement("""
                            INSERT INTO storyblock_spike_commits(worker_id, committed_at_ms)
                            VALUES (?, ?)
                            """)) {
                        statement.setString(1, workerId);
                        statement.setLong(2, System.currentTimeMillis());
                        statement.executeUpdate();
                    }
                    return null;
                });
                completed++;
            } catch (SQLException exception) {
                if (!SqliteMetrics.isBusy(exception) || System.nanoTime() >= deadline) {
                    throw exception;
                }
                Thread.sleep(2L);
            }
        }
        return completed;
    }

    private static long runReader(SqliteDatabase database, int reads) throws Exception {
        long maximum = 0;
        for (int index = 0; index < reads; index++) {
            maximum = Math.max(maximum, countRows(database));
            Thread.sleep(1L);
        }
        return maximum;
    }

    private static long countRows(SqliteDatabase database) throws SQLException {
        return database.readOnly(connection -> {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery(
                         "SELECT COUNT(*) FROM storyblock_spike_commits"
                 )) {
                result.next();
                return result.getLong(1);
            }
        });
    }

    private static Properties load(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        return properties;
    }

    private static long sum(List<Properties> results, String key) {
        return results.stream().mapToLong(result -> property(result, key)).sum();
    }

    private static long max(List<Properties> results, String key) {
        return results.stream().mapToLong(result -> property(result, key)).max().orElse(0L);
    }

    private static long property(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Worker result is missing " + key);
        }
        return Long.parseLong(value);
    }

    private static void sleepUntil(long startAtMillis) throws InterruptedException {
        long delay = startAtMillis - System.currentTimeMillis();
        if (delay > 0) {
            Thread.sleep(delay);
        }
    }

    private static void stopRemainingWorkers(List<WorkerProcess> workers) {
        for (WorkerProcess worker : workers) {
            if (worker.process().isAlive()) {
                worker.process().destroyForcibly();
            }
        }
    }

    private static void cleanupWorkerFiles(List<WorkerProcess> workers, Path directory) {
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

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static void requirePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private record WorkerProcess(
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
