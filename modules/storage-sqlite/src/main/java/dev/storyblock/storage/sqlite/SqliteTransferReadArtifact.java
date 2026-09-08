package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StoredArtifact;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

final class SqliteTransferReadArtifact {
    static StoredArtifact readArtifact(ResultSet result) throws SQLException {
        return new StoredArtifact(
                new Ids.ArtifactId(result.getString("artifact_id")),
                new Ids.NovelId(result.getString("novel_id")),
                new Ids.RevisionId(result.getString("revision_id")),
                result.getString("kind"),
                result.getString("media_type"),
                result.getString("codec"),
                result.getString("content_hash"),
                result.getBytes("content"),
                Instant.parse(result.getString("created_at")),
                result.getInt("portable") == 1
        );
    }
}
