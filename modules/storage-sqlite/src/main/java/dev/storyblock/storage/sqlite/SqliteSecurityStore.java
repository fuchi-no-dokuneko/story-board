package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessKeyInsertResult;
import dev.storyblock.security.AccessKeyRequestConflictException;
import dev.storyblock.security.AccessScope;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;
import dev.storyblock.security.CrossNovelAccessException;
import dev.storyblock.security.MissingAccessKeyException;
import dev.storyblock.security.StoredAccessKey;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class SqliteSecurityStore {
    private SqliteSecurityStore() {
    }

    static AccessKeyInsertResult issueAccessKey(
            Connection connection,
            StoredAccessKey key,
            String idempotencyKey,
            String requestHash,
            AuditContext auditContext
    ) throws SQLException {
        Optional<StoredIssue> prior = findIssue(
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
        insertAuditEvent(connection, AuditEvent.create(
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
                        ? Optional.of(readAccessKey(result))
                        : Optional.empty();
            }
        }
    }

    static boolean revokeAccessKey(
            Connection connection,
            Ids.AccessKeyId keyId,
            Ids.NovelId expectedNovelId,
            AuditContext auditContext
    ) throws SQLException {
        StoredAccessKey key = findAccessKey(connection, keyId)
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
        insertAuditEvent(connection, AuditEvent.create(
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

    static void insertAuditEvent(Connection connection, AuditEvent event)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO audit_events(
                    event_id, occurred_at, request_id, actor_id, actor_key_id,
                    novel_id, action, subject_id, operation_id, revision_id,
                    result, operation_hash, content_hash, event_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, event.eventId().value());
            statement.setString(2, event.occurredAt().toString());
            statement.setString(3, event.requestId());
            statement.setString(4, event.actorId());
            statement.setString(5, event.actorKeyId() == null
                    ? null : event.actorKeyId().value());
            statement.setString(6, event.novelId().value());
            statement.setString(7, event.action().canonicalName());
            statement.setString(8, event.subjectId());
            statement.setString(9, event.operationId() == null
                    ? null : event.operationId().value());
            statement.setString(10, event.revisionId() == null
                    ? null : event.revisionId().value());
            statement.setString(11, event.result().canonicalName());
            statement.setString(12, event.operationHash());
            statement.setString(13, event.contentHash());
            statement.setString(14, event.eventHash());
            statement.executeUpdate();
        }
    }

    static List<AuditEvent> listAuditEvents(
            Connection connection,
            Ids.NovelId novelId
    ) throws SQLException {
        List<AuditEvent> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT event_id, occurred_at, request_id, actor_id, actor_key_id,
                       novel_id, action, subject_id, operation_id, revision_id,
                       result, operation_hash, content_hash, event_hash
                FROM audit_events
                WHERE novel_id = ?
                ORDER BY occurred_at, event_id
                """)) {
            statement.setString(1, novelId.value());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    events.add(readAuditEvent(result));
                }
            }
        }
        return List.copyOf(events);
    }

    private static Optional<StoredIssue> findIssue(
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
                                readAccessKey(result),
                                result.getString("issue_request_hash")
                        ))
                        : Optional.empty();
            }
        }
    }

    private static StoredAccessKey readAccessKey(ResultSet result) throws SQLException {
        return new StoredAccessKey(
                new Ids.AccessKeyId(result.getString("key_id")),
                new Ids.NovelId(result.getString("novel_id")),
                result.getBytes("secret_digest"),
                parseScopes(result.getString("scopes_json")),
                result.getString("actor_id"),
                Instant.parse(result.getString("created_at")),
                Instant.parse(result.getString("expires_at")),
                optionalInstant(result.getString("revoked_at")),
                optionalInstant(result.getString("last_used_at"))
        );
    }

    private static AuditEvent readAuditEvent(ResultSet result) throws SQLException {
        String actorKeyId = result.getString("actor_key_id");
        String operationId = result.getString("operation_id");
        String revisionId = result.getString("revision_id");
        return new AuditEvent(
                new Ids.AuditEventId(result.getString("event_id")),
                Instant.parse(result.getString("occurred_at")),
                result.getString("request_id"),
                result.getString("actor_id"),
                actorKeyId == null ? null : new Ids.AccessKeyId(actorKeyId),
                new Ids.NovelId(result.getString("novel_id")),
                AuditAction.fromCanonicalName(result.getString("action")),
                result.getString("subject_id"),
                operationId == null ? null : new Ids.OperationId(operationId),
                revisionId == null ? null : new Ids.RevisionId(revisionId),
                AuditResult.fromCanonicalName(result.getString("result")),
                result.getString("operation_hash"),
                result.getString("content_hash"),
                result.getString("event_hash")
        );
    }

    private static Set<AccessScope> parseScopes(String json) {
        @SuppressWarnings("unchecked")
        List<String> names = CanonicalJson.mapper().readValue(
                json.getBytes(StandardCharsets.UTF_8), List.class
        );
        Set<AccessScope> scopes = new LinkedHashSet<>();
        for (Object name : names) {
            if (!(name instanceof String value) || !scopes.add(
                    AccessScope.fromCanonicalName(value)
            )) {
                throw new IllegalArgumentException("Stored access scopes are invalid");
            }
        }
        return Set.copyOf(scopes);
    }

    private static Instant optionalInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private record StoredIssue(StoredAccessKey key, String requestHash) {
    }
}
