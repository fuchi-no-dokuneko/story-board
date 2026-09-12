package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.style.StyleProfileVersion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteProfileInsertVersion {
    static void insertVersion(
            Connection connection,
            StyleProfileVersion version
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO style_profile_versions(
                    version_id, profile_id, version, version_json, version_hash,
                    created_by, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, version.versionId().value());
            statement.setString(2, version.profileId().value());
            statement.setInt(3, version.version());
            statement.setString(4, CanonicalJson.string(version.canonicalValue()));
            statement.setString(5, version.versionHash());
            statement.setString(6, version.createdBy());
            statement.setString(7, version.createdAt().toString());
            statement.executeUpdate();
        }
    }
}
