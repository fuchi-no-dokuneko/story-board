package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalNovelPackage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteTransferInsertNovel {
    static void insertNovel(Connection connection, CanonicalNovelPackage document)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO novels(
                    novel_id, head_revision_id, head_sequence, head_hash, schema_version
                ) VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, document.manifest().novelId().value());
            statement.setString(2, document.manifest().headRevisionId().value());
            statement.setLong(3, document.manifest().headSequence());
            statement.setString(4, document.manifest().headHash());
            statement.setString(5, document.manifest().schemaVersion());
            statement.executeUpdate();
        }
    }
}
