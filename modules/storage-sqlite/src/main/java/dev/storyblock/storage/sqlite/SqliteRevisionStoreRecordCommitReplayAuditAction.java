package dev.storyblock.storage.sqlite;

import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditResult;
import dev.storyblock.storage.StoredOperation;
import java.util.Objects;

final class SqliteRevisionStoreRecordCommitReplayAuditAction {
    static void recordCommitReplayAudit(SqliteRevisionStore self, StoredOperation operation, AuditContext auditContext)  {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(auditContext, "auditContext");
        self.write(connection -> {
            SqliteSecurityInsertAuditEvent.insertAuditEvent(connection, SqliteRevisionStoreCommitAuditEvent.commitAuditEvent(
                    operation.operation().context().novelId(),
                    operation.operation().context().operationId(),
                    operation.resultRevisionId(),
                    operation.operationHash(),
                    operation.resultHash(),
                    AuditResult.IDEMPOTENT,
                    auditContext
            ));
            return null;
        });
    }
}
