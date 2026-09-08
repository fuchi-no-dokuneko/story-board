package dev.storyblock.storage.sqlite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import static dev.storyblock.storage.sqlite.SqliteWalSpike.WorkerProcess;

final class SqliteWalSpikeStartWorker {
    static WorkerProcess startWorker(
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
                SqliteWalSpikeIsWindows.isWindows() ? "java.exe" : "java"
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
}
