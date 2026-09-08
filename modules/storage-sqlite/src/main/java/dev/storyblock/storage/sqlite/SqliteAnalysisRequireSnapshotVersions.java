package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StorageException;
import dev.storyblock.style.StyleAnalysisSnapshot;
import java.sql.*;

final class SqliteAnalysisRequireSnapshotVersions {
    static void requireSnapshotVersions(
            Connection connection,
            StyleAnalysisSnapshot snapshot
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT revision.content_hash, version.version_hash
                FROM revisions AS revision
                JOIN style_profile_versions AS version
                  ON version.profile_id = ? AND version.version_id = ?
                WHERE revision.novel_id = ? AND revision.revision_id = ?
                """)) {
            statement.setString(1, snapshot.profileVersion().profileId().value());
            statement.setString(2, snapshot.profileVersion().versionId().value());
            statement.setString(3, snapshot.novelId().value());
            statement.setString(4, snapshot.revisionId().value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()
                        || !snapshot.revisionHash().equals(result.getString(1))
                        || !snapshot.profileVersionHash().equals(result.getString(2))) {
                    throw new StorageException(
                            "Style analysis snapshot versions do not match canonical storage"
                    );
                }
            }
        }
    }
}
