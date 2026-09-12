package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class SqliteRevisionStoreShouldCheckpointAction {
    static boolean shouldCheckpoint(SqliteRevisionStore self, Connection connection, Ids.NovelId novelId, long currentSequence) throws SQLException {
        long checkpointSequence = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(sequence), 0) FROM checkpoints WHERE novel_id = ?"
        )) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                checkpointSequence = result.getLong(1);
            }
        }
        if (currentSequence - checkpointSequence >= self.checkpointPolicy.revisionInterval()) {
            return true;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COALESCE(SUM(length(CAST(payload_json AS BLOB))), 0)
                FROM operations
                WHERE novel_id = ? AND sequence > ? AND sequence <= ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setLong(2, checkpointSequence);
            statement.setLong(3, currentSequence);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1) >= self.checkpointPolicy.replayBytesThreshold();
            }
        }
    }
}
