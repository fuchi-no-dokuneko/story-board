package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

final class SqliteSecurityReadAuditEvent {
    static AuditEvent readAuditEvent(ResultSet result) throws SQLException {
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
}
