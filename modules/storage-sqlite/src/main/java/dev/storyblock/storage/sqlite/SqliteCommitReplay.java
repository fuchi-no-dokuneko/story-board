package dev.storyblock.storage.sqlite;
import dev.storyblock.contracts.*;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditResult;
import dev.storyblock.storage.*;
import java.sql.Connection;
import java.sql.SQLException;

final class SqliteCommitReplay {
  static CommitResult replay(SqliteRevisionStore self, Connection connection, CommitRequest request, AuditContext auditContext, Ids.NovelId novelId, StoredOperation stored) throws SQLException {
      if (!stored.operationHash().equals(request.operationHash())) {
        throw new IdempotencyConflictException(
            request.operation().context().idempotencyKey(),
            stored.operationHash(),
            request.operationHash()
        );
      }
      CommitResult result = new CommitResult(
          new RevisionRef(
              stored.resultRevisionId(), stored.sequence(), stored.resultHash()
          ),
          stored.operation().context().operationId(),
          true
      );
      SqliteSecurityInsertAuditEvent.insertAuditEvent(connection, SqliteRevisionStoreCommitAuditEvent.commitAuditEvent(
          novelId,
          stored.operation().context().operationId(),
          stored.resultRevisionId(),
          stored.operationHash(),
          stored.resultHash(),
          AuditResult.IDEMPOTENT,
          auditContext
      ));
      self.faultInjector.after(CommitStage.AFTER_AUDIT);
      return result;  }
}
