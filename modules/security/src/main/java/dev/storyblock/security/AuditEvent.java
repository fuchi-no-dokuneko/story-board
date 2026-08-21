package dev.storyblock.security;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

public record AuditEvent(
        Ids.AuditEventId eventId,
        Instant occurredAt,
        String requestId,
        String actorId,
        Ids.AccessKeyId actorKeyId,
        Ids.NovelId novelId,
        AuditAction action,
        String subjectId,
        Ids.OperationId operationId,
        Ids.RevisionId revisionId,
        AuditResult result,
        String operationHash,
        String contentHash,
        String eventHash
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public AuditEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        requestId = SecurityIdentifier.require(requestId, "Request ID");
        actorId = SecurityIdentifier.require(actorId, "Actor ID");
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(action, "action");
        subjectId = SecurityIdentifier.optional(subjectId, "Subject ID");
        Objects.requireNonNull(result, "result");
        requireOptionalHash(operationHash, "Operation hash");
        requireOptionalHash(contentHash, "Content hash");
        if (eventHash == null || !HASH.matcher(eventHash).matches()) {
            throw new IllegalArgumentException("Event hash must be lowercase SHA-256");
        }
        String expectedHash = calculateHash(
                eventId,
                occurredAt,
                requestId,
                actorId,
                actorKeyId,
                novelId,
                action,
                subjectId,
                operationId,
                revisionId,
                result,
                operationHash,
                contentHash
        );
        if (!expectedHash.equals(eventHash)) {
            throw new IllegalArgumentException("Audit event hash does not match its fields");
        }
    }

    public static AuditEvent create(
            AuditContext context,
            Ids.NovelId novelId,
            AuditAction action,
            String subjectId,
            Ids.OperationId operationId,
            Ids.RevisionId revisionId,
            AuditResult result,
            String operationHash,
            String contentHash
    ) {
        Ids.AuditEventId eventId = Ids.AuditEventId.create();
        String eventHash = calculateHash(
                eventId,
                context.occurredAt(),
                context.requestId(),
                context.actorId(),
                context.actorKeyId(),
                novelId,
                action,
                subjectId,
                operationId,
                revisionId,
                result,
                operationHash,
                contentHash
        );
        return new AuditEvent(
                eventId,
                context.occurredAt(),
                context.requestId(),
                context.actorId(),
                context.actorKeyId(),
                novelId,
                action,
                subjectId,
                operationId,
                revisionId,
                result,
                operationHash,
                contentHash,
                eventHash
        );
    }

    private static String calculateHash(
            Ids.AuditEventId eventId,
            Instant occurredAt,
            String requestId,
            String actorId,
            Ids.AccessKeyId actorKeyId,
            Ids.NovelId novelId,
            AuditAction action,
            String subjectId,
            Ids.OperationId operationId,
            Ids.RevisionId revisionId,
            AuditResult result,
            String operationHash,
            String contentHash
    ) {
        Map<String, Object> hashInput = new TreeMap<>();
        hashInput.put("event_id", eventId.value());
        hashInput.put("occurred_at", occurredAt.toString());
        hashInput.put("request_id", requestId);
        hashInput.put("actor_id", actorId);
        hashInput.put("actor_key_id", actorKeyId == null ? null : actorKeyId.value());
        hashInput.put("novel_id", novelId.value());
        hashInput.put("action", action.canonicalName());
        hashInput.put("subject_id", subjectId);
        hashInput.put("operation_id", operationId == null ? null : operationId.value());
        hashInput.put("revision_id", revisionId == null ? null : revisionId.value());
        hashInput.put("result", result.canonicalName());
        hashInput.put("operation_hash", operationHash);
        hashInput.put("content_hash", contentHash);
        return CanonicalJson.hash(hashInput);
    }

    private static void requireOptionalHash(String value, String field) {
        if (value != null && !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
    }
}
