package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StoredOperation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

final class SqliteRevisionStoreListOperationsAction {
    static List<StoredOperation> listOperations(SqliteRevisionStore self, Ids.NovelId novelId, long afterSequence, long throughSequence)  {
        if (afterSequence < 0 || throughSequence < afterSequence) {
            throw new IllegalArgumentException("Invalid operation sequence range");
        }
        return self.read(connection -> {
            List<StoredOperation> operations = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT operation_id, novel_id, base_revision_id, operation_type,
                           idempotency_key, sequence, operation_hash, payload_json,
                           result_revision_id, result_hash, committed_at
                    FROM operations
                    WHERE novel_id = ? AND sequence > ? AND sequence <= ?
                    ORDER BY sequence
                    """)) {
                statement.setString(1, novelId.value());
                statement.setLong(2, afterSequence);
                statement.setLong(3, throughSequence);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        operations.add(SqliteRevisionStoreReadOperation.readOperation(result));
                    }
                }
            }
            return List.copyOf(operations);
        });
    }
}
