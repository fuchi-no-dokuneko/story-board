package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StorageException;
import dev.storyblock.storage.StoredOperation;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

final class SqliteRevisionStoreReadOperation {
    static StoredOperation readOperation(ResultSet result) throws SQLException {
        EditOperation operation = EditOperationCanonicalMapper.fromCanonical(
                result.getString("payload_json").getBytes(StandardCharsets.UTF_8)
        );
        String storedHash = result.getString("operation_hash");
        if (!EditOperationCanonicalMapper.hash(operation).equals(storedHash)) {
            throw new StorageException(
                    "Stored operation hash does not match " + operation.context().operationId().value()
            );
        }
        if (!operation.context().operationId().value().equals(result.getString("operation_id"))
                || !operation.context().novelId().value().equals(result.getString("novel_id"))
                || !operation.context().baseRevisionId().value().equals(
                        result.getString("base_revision_id")
                )
                || !operation.type().canonicalName().equals(result.getString("operation_type"))
                || !operation.context().idempotencyKey().equals(
                        result.getString("idempotency_key")
                )) {
            throw new StorageException("Stored operation relational identity does not match payload");
        }
        return new StoredOperation(
                operation,
                result.getLong("sequence"),
                storedHash,
                new Ids.RevisionId(result.getString("result_revision_id")),
                result.getString("result_hash"),
                Instant.parse(result.getString("committed_at"))
        );
    }
}
