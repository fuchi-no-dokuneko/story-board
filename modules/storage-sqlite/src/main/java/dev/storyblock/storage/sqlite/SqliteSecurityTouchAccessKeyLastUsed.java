package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;

final class SqliteSecurityTouchAccessKeyLastUsed {
    static boolean touchAccessKeyLastUsed(
            Connection connection,
            Ids.AccessKeyId keyId,
            Instant usedAt,
            Instant staleBefore
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE access_keys
                SET last_used_at = ?
                WHERE key_id = ?
                  AND revoked_at IS NULL
                  AND (last_used_at IS NULL OR last_used_at <= ?)
                """)) {
            statement.setString(1, usedAt.toString());
            statement.setString(2, keyId.value());
            statement.setString(3, staleBefore.toString());
            return statement.executeUpdate() == 1;
        }
    }
}
