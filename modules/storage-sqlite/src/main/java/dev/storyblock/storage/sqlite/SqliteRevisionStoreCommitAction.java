package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditResult;
import dev.storyblock.storage.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

final class SqliteRevisionStoreCommitAction {
  static CommitResult commit(SqliteRevisionStore self, Connection connection, CommitRequest request, AuditContext auditContext) throws SQLException {
    Ids.NovelId novelId = request.operation().context().novelId();
    Optional<StoredOperation> prior = SqliteRevisionStoreFindByIdempotencyKeyFactory.findByIdempotencyKey(
        connection, novelId, request.operation().context().idempotencyKey()
    );
    self.faultInjector.after(CommitStage.AFTER_IDEMPOTENCY_CHECK);
    if (prior.isPresent()) {
      return SqliteCommitReplay.replay(self, connection, request, auditContext, novelId, prior.get());
    }

    RevisionRef actualHead = SqliteRevisionStoreRequireHead.requireHead(connection, novelId);
    if (!actualHead.equals(request.expectedHead())) {
      throw new StaleHeadException(request.expectedHead(), actualHead);
    }
    long sequence = actualHead.sequence() + 1;
    byte[] operationBytes = CanonicalJson.bytes(
        EditOperationCanonicalMapper.toCanonical(request.operation())
    );
    byte[] candidateBytes = NarrativeCanonicalMapper.toCanonical(request.candidate()).envelopeBytes();

    SqliteRevisionStoreInsertOperation.insertOperation(connection, request, sequence, operationBytes);
    self.faultInjector.after(CommitStage.AFTER_OPERATION_APPEND);
    SqliteRevisionStoreInsertRevision.insertRevision(
        connection,
        request.candidate(),
        sequence,
        request.candidateHash(),
        candidateBytes,
        request.operation().context().operationId()
    );
    self.faultInjector.after(CommitStage.AFTER_REVISION_APPEND);
    SqliteCommitProjection.update(self, connection, request, novelId, sequence, candidateBytes);
    self.faultInjector.after(CommitStage.BEFORE_HEAD_CAS);

    SqliteHeadCas.update(connection, request, novelId, sequence);
    SqliteSecurityInsertAuditEvent.insertAuditEvent(connection, SqliteRevisionStoreCommitAuditEvent.commitAuditEvent(
        novelId,
        request.operation().context().operationId(),
        request.candidate().id(),
        request.operationHash(),
        request.candidateHash(),
        AuditResult.SUCCEEDED,
        auditContext
    ));
    self.faultInjector.after(CommitStage.AFTER_AUDIT);
    return new CommitResult(
        new RevisionRef(request.candidate().id(), sequence, request.candidateHash()),
        request.operation().context().operationId(),
        false
    );
  }
}
