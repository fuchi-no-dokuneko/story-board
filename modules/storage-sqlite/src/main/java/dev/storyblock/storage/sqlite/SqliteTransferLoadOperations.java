package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class SqliteTransferLoadOperations {
    static List<CanonicalNovelPackage.OperationEntry> loadOperations(
            Connection connection,
            Ids.NovelId novelId,
            long throughSequence
    ) throws SQLException {
        List<CanonicalNovelPackage.OperationEntry> operations = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT operation_id, novel_id, base_revision_id, operation_type,
                       idempotency_key, sequence, operation_hash, payload_json,
                       result_revision_id, result_hash, committed_at
                FROM operations
                WHERE novel_id = ? AND sequence <= ?
                ORDER BY sequence
                """)) {
            statement.setString(1, novelId.value());
            statement.setLong(2, throughSequence);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    EditOperation operation = SqliteTransferReadOperation.readOperation(result);
                    operations.add(new CanonicalNovelPackage.OperationEntry(
                            result.getLong("sequence"),
                            result.getString("operation_hash"),
                            operation,
                            new Ids.RevisionId(result.getString("result_revision_id")),
                            result.getString("result_hash"),
                            Instant.parse(result.getString("committed_at"))
                    ));
                }
            }
        }
        return List.copyOf(operations);
    }
}
