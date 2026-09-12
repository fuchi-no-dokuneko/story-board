package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.*;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteTransferData.*;

final class SqliteTransferFindExportByIdempotencyKey {
    static Optional<StoredExportWithHash> findExportByIdempotencyKey(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT job_id, novel_id, revision_id, revision_sequence, revision_hash,
                       format, result_artifact_id, created_at, request_hash
                FROM export_jobs
                WHERE novel_id = ? AND idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(new StoredExportWithHash(
                                SqliteTransferReadExportJob.readExportJob(result), result.getString("request_hash")
                        ))
                        : Optional.empty();
            }
        }
    }
}
