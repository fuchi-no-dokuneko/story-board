package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.storage.StorageException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;

final class SqliteTransferReadOperation {
    static EditOperation readOperation(ResultSet result) throws SQLException {
        EditOperation operation = EditOperationCanonicalMapper.fromCanonical(
                result.getString("payload_json").getBytes(StandardCharsets.UTF_8)
        );
        String storedHash = result.getString("operation_hash");
        if (!EditOperationCanonicalMapper.hash(operation).equals(storedHash)
                || !operation.context().operationId().value().equals(
                        result.getString("operation_id")
                )
                || !operation.context().novelId().value().equals(result.getString("novel_id"))
                || !operation.context().baseRevisionId().value().equals(
                        result.getString("base_revision_id")
                )
                || !operation.type().canonicalName().equals(result.getString("operation_type"))
                || !operation.context().idempotencyKey().equals(
                        result.getString("idempotency_key")
                )) {
            throw new StorageException("Stored operation identity does not match package data");
        }
        return operation;
    }
}
