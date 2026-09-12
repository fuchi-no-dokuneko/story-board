package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class SqliteSecurityRevokeAccessKey {
    static boolean revokeAccessKey(
            Connection connection,
            Ids.AccessKeyId keyId,
            Ids.NovelId expectedNovelId,
            AuditContext auditContext
    ) throws SQLException {
        StoredAccessKey key = SqliteSecurityFindAccessKey.findAccessKey(connection, keyId)
                .orElseThrow(() -> new MissingAccessKeyException(keyId));
        if (!key.novelId().equals(expectedNovelId)) {
            throw new CrossNovelAccessException();
        }
        int changed;
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE access_keys
                SET revoked_at = ?
                WHERE key_id = ? AND revoked_at IS NULL
                """)) {
            statement.setString(1, auditContext.occurredAt().toString());
            statement.setString(2, keyId.value());
            changed = statement.executeUpdate();
        }
        SqliteSecurityInsertAuditEvent.insertAuditEvent(connection, AuditEvent.create(
                auditContext,
                key.novelId(),
                AuditAction.ACCESS_KEY_REVOKE,
                keyId.value(),
                null,
                null,
                changed == 1 ? AuditResult.SUCCEEDED : AuditResult.IDEMPOTENT,
                null,
                null
        ));
        return changed == 1;
    }
}
