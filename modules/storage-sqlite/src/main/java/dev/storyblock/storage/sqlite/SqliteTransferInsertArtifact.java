package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StoredArtifact;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteTransferInsertArtifact {
    static void insertArtifact(Connection connection, StoredArtifact artifact)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO artifacts(
                    artifact_id, novel_id, revision_id, kind, media_type, codec,
                    content_hash, size_bytes, content, created_at, portable
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, artifact.artifactId().value());
            statement.setString(2, artifact.novelId().value());
            statement.setString(3, artifact.revisionId().value());
            statement.setString(4, artifact.kind());
            statement.setString(5, artifact.mediaType());
            statement.setString(6, artifact.codec());
            statement.setString(7, artifact.contentHash());
            statement.setInt(8, artifact.content().length);
            statement.setBytes(9, artifact.content());
            statement.setString(10, artifact.createdAt().toString());
            statement.setInt(11, artifact.portable() ? 1 : 0);
            statement.executeUpdate();
        }
    }
}
