package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalNovelPackage;
import dev.storyblock.domain.Ids;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

final class SqliteTransferLoadPortableArtifacts {
    static List<CanonicalNovelPackage.ArtifactEntry> loadPortableArtifacts(
            Connection connection,
            Ids.NovelId novelId
    ) throws SQLException {
        List<CanonicalNovelPackage.ArtifactEntry> artifacts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT artifact_id, novel_id, revision_id, kind, media_type, codec,
                       content_hash, content, created_at, portable
                FROM artifacts
                WHERE novel_id = ? AND portable = 1
                ORDER BY artifact_id
                """)) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    artifacts.add(SqliteTransferReadArtifact.readArtifact(result).toPackageEntry());
                }
            }
        }
        return List.copyOf(artifacts);
    }
}
