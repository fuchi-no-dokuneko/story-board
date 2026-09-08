package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StorageException;
import dev.storyblock.style.MissingStyleProfileException;
import dev.storyblock.style.StyleProfile;
import java.sql.*;

final class SqliteProfileGetProfile {
    static StyleProfile getProfile(
            Connection connection,
            Ids.StyleProfileId profileId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT novel_id, profile_json, resource_hash, created_by, created_at
                FROM style_profiles
                WHERE profile_id = ?
                """)) {
            statement.setString(1, profileId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MissingStyleProfileException(profileId);
                }
                StyleProfile profile = StyleProfile.fromCanonical(SqliteProfileParseObject.parseObject(
                        result.getString("profile_json"), "style profile"
                ));
                if (!profile.profileId().equals(profileId)
                        || !profile.scope().novelId().value().equals(
                                result.getString("novel_id")
                        )
                        || !profile.resourceHash().equals(result.getString("resource_hash"))
                        || !profile.createdBy().equals(result.getString("created_by"))
                        || !profile.createdAt().toString().equals(result.getString("created_at"))) {
                    throw new StorageException("Stored style profile integrity check failed");
                }
                return profile;
            }
        }
    }
}
