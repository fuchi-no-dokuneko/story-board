package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.storage.StorageException;
import java.sql.*;

final class SqliteProfileNextVersionNumber {
    static int nextVersionNumber(
            Connection connection,
            Ids.StyleProfileId profileId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COALESCE(MAX(version), 0) + 1
                FROM style_profile_versions
                WHERE profile_id = ?
                """)) {
            statement.setString(1, profileId.value());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new StorageException("Could not allocate style profile version");
                }
                return result.getInt(1);
            }
        }
    }
}
