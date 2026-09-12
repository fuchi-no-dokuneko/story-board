package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;

final class SqliteRevisionStoreCommitAuditEvent {
    static AuditEvent commitAuditEvent(
            Ids.NovelId novelId,
            Ids.OperationId operationId,
            Ids.RevisionId revisionId,
            String operationHash,
            String contentHash,
            AuditResult result,
            AuditContext context
    ) {
        return AuditEvent.create(
                context,
                novelId,
                AuditAction.COMMIT,
                operationId.value(),
                operationId,
                revisionId,
                result,
                operationHash,
                contentHash
        );
    }
}
