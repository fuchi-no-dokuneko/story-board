package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StorageException;
import dev.storyblock.style.MissingStyleProfileVersionException;
import dev.storyblock.style.StyleProfileVersion;
import dev.storyblock.style.StyleProfileVersionView;
import java.sql.*;

final class SqliteProfileGetVersion {
    static StyleProfileVersionView getVersion(
            Connection connection,
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId
    ) throws SQLException {
        StyleProfileVersion version;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT version_json, version_hash, created_by, created_at
                FROM style_profile_versions
                WHERE profile_id = ? AND version_id = ?
                """)) {
            statement.setString(1, profileId.value());
            statement.setString(2, versionId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingStyleProfileVersionException(profileId, versionId);
                }
                version = StyleProfileVersion.fromCanonical(SqliteProfileParseObject.parseObject(
                        result.getString("version_json"), "style profile version"
                ));
                if (!version.profileId().equals(profileId)
                        || !version.versionId().equals(versionId)
                        || !version.versionHash().equals(result.getString("version_hash"))
                        || !version.createdBy().equals(result.getString("created_by"))
                        || !version.createdAt().toString().equals(result.getString("created_at"))) {
                    throw new StorageException(
                            "Stored style profile version integrity check failed"
                    );
                }
            }
        }
        return StyleProfileVersionView.of(
                version, SqliteProfileLifecycle.lifecycle(connection, profileId, versionId)
        );
    }
}
