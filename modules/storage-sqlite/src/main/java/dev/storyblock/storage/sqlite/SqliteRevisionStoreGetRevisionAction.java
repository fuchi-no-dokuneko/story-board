package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.MissingRevisionException;
import dev.storyblock.storage.StoredRevision;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

final class SqliteRevisionStoreGetRevisionAction {
    static StoredRevision getRevision(SqliteRevisionStore self, Ids.NovelId novelId, Ids.RevisionId revisionId)  {
        return self.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT parent_revision_id, sequence, content_hash,
                           canonical_json, created_at
                    FROM revisions
                    WHERE novel_id = ? AND revision_id = ?
                    """)) {
                statement.setString(1, novelId.value());
                statement.setString(2, revisionId.value());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new MissingRevisionException(novelId, revisionId);
                    }
                    return SqliteRevisionStoreReadRevision.readRevision(result, novelId, revisionId);
                }
            }
        });
    }
}
