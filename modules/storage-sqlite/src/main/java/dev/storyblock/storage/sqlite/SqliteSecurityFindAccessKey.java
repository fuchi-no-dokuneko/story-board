package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.StoredAccessKey;
import java.sql.*;
import java.util.Optional;

final class SqliteSecurityFindAccessKey {
    static Optional<StoredAccessKey> findAccessKey(
            Connection connection,
            Ids.AccessKeyId keyId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT key_id, novel_id, secret_digest, scopes_json, actor_id,
                       created_at, expires_at, revoked_at, last_used_at
                FROM access_keys
                WHERE key_id = ?
                """)) {
            statement.setString(1, keyId.value());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(SqliteSecurityReadAccessKey.readAccessKey(result))
                        : Optional.empty();
            }
        }
    }
}
