package dev.storyblock.storage.sqlite;

import java.nio.file.Path;
import static dev.storyblock.storage.sqlite.SqliteWalSpike.Report;

final class SqliteWalSpikeMainFactory {
    static void main(String[] arguments) throws Exception {
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
        Report report = SqliteWalSpike.runMultiProcess(Path.of(arguments[0]), 2, 2, writesPerProcess, 200);
        System.out.println(report.toJson());
    }
}
