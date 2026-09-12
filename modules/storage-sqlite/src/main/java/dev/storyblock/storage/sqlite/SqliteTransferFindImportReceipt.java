package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.*;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteTransferData.*;

final class SqliteTransferFindImportReceipt {
    static Optional<ImportReceipt> findImportReceipt(
            Connection connection,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT request_hash, novel_id
                FROM import_receipts
                WHERE idempotency_key = ?
                """)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(new ImportReceipt(
                                result.getString("request_hash"),
                                new Ids.NovelId(result.getString("novel_id"))
                        ))
                        : Optional.empty();
            }
        }
    }
}
