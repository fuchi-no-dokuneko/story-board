package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.CanonicalImportRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteTransferInsertImportReceipt {
    static void insertImportReceipt(
            Connection connection,
            CanonicalImportRequest request,
            Ids.NovelId novelId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO import_receipts(
                    idempotency_key, request_hash, novel_id, imported_at
                ) VALUES (?, ?, ?, ?)
                """)) {
            statement.setString(1, request.idempotencyKey());
            statement.setString(2, request.requestHash());
            statement.setString(3, novelId.value());
            statement.setString(4, request.importedAt().toString());
            statement.executeUpdate();
        }
    }
}
