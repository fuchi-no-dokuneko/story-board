package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteTransferInsertOperations {
    static void insertOperations(Connection connection, CanonicalNovelPackage document)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO operations(
                    operation_id, novel_id, sequence, base_revision_id, operation_type,
                    operation_hash, idempotency_key, payload_json, result_revision_id,
                    result_hash, committed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (CanonicalNovelPackage.OperationEntry entry : document.operations()) {
                EditOperation operation = entry.operation();
                statement.setString(1, operation.context().operationId().value());
                statement.setString(2, operation.context().novelId().value());
                statement.setLong(3, entry.sequence());
                statement.setString(4, operation.context().baseRevisionId().value());
                statement.setString(5, operation.type().canonicalName());
                statement.setString(6, entry.operationHash());
                statement.setString(7, operation.context().idempotencyKey());
                statement.setString(8, CanonicalJson.string(
                        EditOperationCanonicalMapper.toCanonical(operation)
                ));
                statement.setString(9, entry.resultRevisionId().value());
                statement.setString(10, entry.resultHash());
                statement.setString(11, entry.committedAt().toString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
