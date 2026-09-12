package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.RevisionRef;
import java.sql.*;

final class SqliteTransferRequireRevision {
    static RevisionRef requireRevision(
            Connection connection,
            Ids.NovelId novelId,
            Ids.RevisionId revisionId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sequence, content_hash
                FROM revisions
                WHERE novel_id = ? AND revision_id = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, revisionId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new dev.storyblock.storage.MissingRevisionException(
                            novelId, revisionId
                    );
                }
                return new RevisionRef(
                        revisionId,
                        result.getLong("sequence"),
                        result.getString("content_hash")
                );
            }
        }
    }
}
