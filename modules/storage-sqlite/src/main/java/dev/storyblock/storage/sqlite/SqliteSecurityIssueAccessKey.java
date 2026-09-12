package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.security.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteSecurityData.*;

final class SqliteSecurityIssueAccessKey {
    static AccessKeyInsertResult issueAccessKey(
            Connection connection,
            StoredAccessKey key,
            String idempotencyKey,
            String requestHash,
            AuditContext auditContext
    ) throws SQLException {
        Optional<StoredIssue> prior = SqliteSecurityFindIssue.findIssue(
                connection, key.novelId(), idempotencyKey
        );
        if (prior.isPresent()) {
            if (!prior.get().requestHash().equals(requestHash)) {
                throw new AccessKeyRequestConflictException();
            }
            return new AccessKeyInsertResult(prior.get().key(), true);
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO access_keys(
                    key_id, novel_id, secret_digest, scopes_json, actor_id,
                    created_at, expires_at, revoked_at, last_used_at,
                    issue_idempotency_key, issue_request_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?)
                """)) {
            statement.setString(1, key.keyId().value());
            statement.setString(2, key.novelId().value());
            statement.setBytes(3, key.secretDigest());
            statement.setString(4, CanonicalJson.string(
                    AccessScope.canonicalNames(key.scopes())
            ));
            statement.setString(5, key.actorId());
            statement.setString(6, key.createdAt().toString());
            statement.setString(7, key.expiresAt().toString());
            statement.setString(8, idempotencyKey);
            statement.setString(9, requestHash);
            statement.executeUpdate();
        }
        SqliteSecurityInsertAuditEvent.insertAuditEvent(connection, AuditEvent.create(
                auditContext,
                key.novelId(),
                AuditAction.ACCESS_KEY_ISSUE,
                key.keyId().value(),
                null,
                null,
                AuditResult.SUCCEEDED,
                null,
                requestHash
        ));
        return new AccessKeyInsertResult(key, false);
    }
}
