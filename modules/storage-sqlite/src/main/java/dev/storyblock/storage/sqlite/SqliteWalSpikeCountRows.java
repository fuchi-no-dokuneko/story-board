package dev.storyblock.storage.sqlite;

import java.sql.SQLException;

final class SqliteWalSpikeCountRows {
    static long countRows(SqliteDatabase database) throws SQLException {
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
}
