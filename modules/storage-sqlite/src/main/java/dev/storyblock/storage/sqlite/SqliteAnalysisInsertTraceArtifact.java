package dev.storyblock.storage.sqlite;

import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisTrace;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteAnalysisInsertTraceArtifact {
    static void insertTraceArtifact(
            Connection connection,
            StyleAnalysisJob job,
            StyleAnalysisTrace trace
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO artifacts(
                    artifact_id, novel_id, revision_id, kind, media_type, codec,
                    content_hash, size_bytes, content, created_at, portable
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """)) {
            statement.setString(1, trace.artifactId().value());
            statement.setString(2, job.snapshot().novelId().value());
            statement.setString(3, job.snapshot().revisionId().value());
            statement.setString(4, StyleAnalysisTrace.KIND);
            statement.setString(5, StyleAnalysisTrace.MEDIA_TYPE);
            statement.setString(6, StyleAnalysisTrace.CODEC);
            statement.setString(7, trace.contentHash());
            statement.setInt(8, trace.compressedContent().length);
            statement.setBytes(9, trace.compressedContent());
            statement.setString(10, trace.createdAt().toString());
            statement.executeUpdate();
        }
    }
}
