package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.MissingNovelException;
import dev.storyblock.storage.RevisionRef;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class SqliteRevisionStoreRequireHead {
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
                    throw new MissingNovelException(novelId);
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
