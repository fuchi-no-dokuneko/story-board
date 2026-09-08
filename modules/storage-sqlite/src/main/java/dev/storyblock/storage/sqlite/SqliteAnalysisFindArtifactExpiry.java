package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

final class SqliteAnalysisFindArtifactExpiry {
    static Optional<Instant> findArtifactExpiry(
            Connection connection,
            Ids.ArtifactId artifactId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT expires_at
                FROM analysis_artifacts
                WHERE artifact_id = ?
                """)) {
            statement.setString(1, artifactId.value());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(Instant.parse(result.getString(1)))
                        : Optional.empty();
            }
        }
    }
}
