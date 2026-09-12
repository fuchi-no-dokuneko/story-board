package dev.storyblock.storage.sqlite;

import dev.storyblock.style.StyleAnalysisTrace;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteAnalysisInsertAnalysisArtifact {
    static void insertAnalysisArtifact(
            Connection connection,
            StyleAnalysisTrace trace
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO analysis_artifacts(
                    artifact_id, analysis_id, expires_at, uncompressed_bytes
                ) VALUES (?, ?, ?, ?)
                """)) {
            statement.setString(1, trace.artifactId().value());
            statement.setString(2, trace.analysisId().value());
            statement.setString(3, trace.expiresAt().toString());
            statement.setInt(4, trace.uncompressedBytes());
            statement.executeUpdate();
        }
    }
}
