package dev.storyblock.security;

import dev.storyblock.domain.Ids;

final class AuditEventCreateFactory {
    static AuditEvent create(AuditContext context, Ids.NovelId novelId, AuditAction action, String subjectId, Ids.OperationId operationId, Ids.RevisionId revisionId, AuditResult result, String operationHash, String contentHash)  {
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
