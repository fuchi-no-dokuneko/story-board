package dev.storyblock.security;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Objects;
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
    static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public AuditEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        requestId = SecurityIdentifier.require(requestId, "Request ID");
        actorId = SecurityIdentifier.require(actorId, "Actor ID");
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(action, "action");
        subjectId = SecurityIdentifier.optional(subjectId, "Subject ID");
        Objects.requireNonNull(result, "result");
        AuditEventRequireOptionalHash.requireOptionalHash(operationHash, "Operation hash");
        AuditEventRequireOptionalHash.requireOptionalHash(contentHash, "Content hash");
        if (eventHash == null || !HASH.matcher(eventHash).matches()) {
            throw new IllegalArgumentException("Event hash must be lowercase SHA-256");
        }
        String expectedHash = AuditEventCalculateHash.calculateHash(
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
        String eventHash = AuditEventCalculateHash.calculateHash(
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

}
