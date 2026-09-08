package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StoredCheckpoint;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

final class SqliteRevisionStoreLoadCheckpointAction {
    static Optional<StoredCheckpoint> loadCheckpoint(SqliteRevisionStore self, Ids.NovelId novelId, long atOrBeforeSequence)  {
        return self.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT revision_id, sequence, content_hash, codec,
                           uncompressed_bytes, compressed_json
                    FROM checkpoints
                    WHERE novel_id = ? AND sequence <= ?
                    ORDER BY sequence DESC
                    LIMIT 1
                    """)) {
                statement.setString(1, novelId.value());
                statement.setLong(2, atOrBeforeSequence);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new StoredCheckpoint(
                            novelId,
                            new Ids.RevisionId(result.getString("revision_id")),
                            result.getLong("sequence"),
                            result.getString("content_hash"),
                            result.getString("codec"),
                            result.getInt("uncompressed_bytes"),
                            result.getBytes("compressed_json")
                    ));
                }
            }
        });
    }
}
