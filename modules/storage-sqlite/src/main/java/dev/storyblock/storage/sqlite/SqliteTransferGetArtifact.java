package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.MissingArtifactException;
import dev.storyblock.storage.StoredArtifact;
import java.sql.*;

final class SqliteTransferGetArtifact {
    static StoredArtifact getArtifact(Connection connection, Ids.ArtifactId artifactId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT artifact_id, novel_id, revision_id, kind, media_type, codec,
                       content_hash, content, created_at, portable
                FROM artifacts
                WHERE artifact_id = ?
                """)) {
            statement.setString(1, artifactId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingArtifactException(artifactId);
                }
                return SqliteTransferReadArtifact.readArtifact(result);
            }
        }
    }
}
