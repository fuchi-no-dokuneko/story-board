package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StorageException;
import dev.storyblock.storage.StoredRevision;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

final class SqliteRevisionStoreGetRevisionAtSequenceAction {
    static StoredRevision getRevisionAtSequence(SqliteRevisionStore self, Ids.NovelId novelId, long sequence)  {
        return self.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT revision_id, parent_revision_id, sequence, content_hash,
                           canonical_json, created_at
                    FROM revisions
                    WHERE novel_id = ? AND sequence = ?
                    """)) {
                statement.setString(1, novelId.value());
                statement.setLong(2, sequence);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new StorageException(
                                "Novel " + novelId.value() + " has no revision sequence " + sequence
                        );
                    }
                    return SqliteRevisionStoreReadRevision.readRevision(
                            result, novelId, new Ids.RevisionId(result.getString("revision_id"))
                    );
                }
            }
        });
    }
}
