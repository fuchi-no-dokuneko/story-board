package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import java.sql.*;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteSecurityData.*;

final class SqliteSecurityFindIssue {
    static Optional<StoredIssue> findIssue(
            Connection connection,
            Ids.NovelId novelId,
            String idempotencyKey
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT key_id, novel_id, secret_digest, scopes_json, actor_id,
                       created_at, expires_at, revoked_at, last_used_at,
                       issue_request_hash
                FROM access_keys
                WHERE novel_id = ? AND issue_idempotency_key = ?
                """)) {
            statement.setString(1, novelId.value());
            statement.setString(2, idempotencyKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(new StoredIssue(
                                SqliteSecurityReadAccessKey.readAccessKey(result),
                                result.getString("issue_request_hash")
                        ))
                        : Optional.empty();
            }
        }
    }
}
