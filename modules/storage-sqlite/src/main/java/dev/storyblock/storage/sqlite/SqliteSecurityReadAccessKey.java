package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.StoredAccessKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

final class SqliteSecurityReadAccessKey {
    static StoredAccessKey readAccessKey(ResultSet result) throws SQLException {
        return new StoredAccessKey(
                new Ids.AccessKeyId(result.getString("key_id")),
                new Ids.NovelId(result.getString("novel_id")),
                result.getBytes("secret_digest"),
                SqliteSecurityParseScopes.parseScopes(result.getString("scopes_json")),
                result.getString("actor_id"),
                Instant.parse(result.getString("created_at")),
                Instant.parse(result.getString("expires_at")),
                SqliteSecurityOptionalInstant.optionalInstant(result.getString("revoked_at")),
                SqliteSecurityOptionalInstant.optionalInstant(result.getString("last_used_at"))
        );
    }
}
