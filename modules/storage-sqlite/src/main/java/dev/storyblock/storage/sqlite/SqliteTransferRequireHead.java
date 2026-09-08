package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.RevisionRef;
import java.sql.*;

final class SqliteTransferRequireHead {
    static RevisionRef requireHead(Connection connection, Ids.NovelId novelId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT head_revision_id, head_sequence, head_hash
                FROM novels
                WHERE novel_id = ?
                """)) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new dev.storyblock.storage.MissingNovelException(novelId);
                }
                return new RevisionRef(
                        new Ids.RevisionId(result.getString("head_revision_id")),
                        result.getLong("head_sequence"),
                        result.getString("head_hash")
                );
            }
        }
    }
}
