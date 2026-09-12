package dev.storyblock.storage.sqlite;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

final class SqliteWalSpikeInitialize {
    static void initialize(Path databasePath) throws IOException, SQLException {
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
}
