package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.storage.CommitRequest;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteRevisionStoreInsertOperation {
    static void insertOperation(
            Connection connection,
            CommitRequest request,
            long sequence,
            byte[] operationBytes
    ) throws SQLException {
        EditOperation operation = request.operation();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO operations(
                    operation_id, novel_id, sequence, base_revision_id, operation_type,
                    operation_hash, idempotency_key, payload_json, result_revision_id,
                    result_hash, committed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, operation.context().operationId().value());
            statement.setString(2, operation.context().novelId().value());
            statement.setLong(3, sequence);
            statement.setString(4, operation.context().baseRevisionId().value());
            statement.setString(5, operation.type().canonicalName());
            statement.setString(6, request.operationHash());
            statement.setString(7, operation.context().idempotencyKey());
            statement.setString(8, new String(operationBytes, StandardCharsets.UTF_8));
            statement.setString(9, request.candidate().id().value());
            statement.setString(10, request.candidateHash());
            statement.setString(11, request.candidate().createdAt().toString());
            statement.executeUpdate();
        }
    }
}
