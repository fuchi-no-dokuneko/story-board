package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StoredOperation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

final class SqliteRevisionStoreFindByIdempotencyKeyFactory {
    static Optional<StoredOperation> findByIdempotencyKey(Connection connection, Ids.NovelId novelId, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT operation_id, novel_id, base_revision_id, operation_type,
                       idempotency_key, sequence, operation_hash, payload_json,
                       result_revision_id, result_hash, committed_at
                FROM operations
                WHERE novel_id = ? AND idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, key);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(SqliteRevisionStoreReadOperation.readOperation(result)) : Optional.empty();
            }
        }
    }
}
