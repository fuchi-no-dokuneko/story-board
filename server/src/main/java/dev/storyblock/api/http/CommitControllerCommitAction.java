package dev.storyblock.api.http;

import dev.storyblock.application.CommitRejectedException;
import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.*;
import dev.storyblock.storage.CommitResult;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;

final class CommitControllerCommitAction {
  static ResponseEntity<Map<String, Object>> commit(CommitController self, String novelId, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication, HttpServletRequest servletRequest)  {
    Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
    AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
    Map<String, Object> request = StrictJsonRequest.parseObject(
        requestBytes, "commit request"
    );
    StrictJsonRequest.requireKeys(
        request,
        Set.of("operation", "candidate_revision_id", "candidate_created_at"),
        "commit request"
    );
    EditOperation operation = EditOperationCanonicalMapper.fromCanonical(
        StrictJsonRequest.object(request.get("operation"), "commit request.operation")
    );
    if (!operation.context().novelId().equals(requestedNovel)) {
      throw new CrossNovelAccessException();
    }
    if (!operation.context().idempotencyKey().equals(idempotencyKey)) {
      throw new IllegalArgumentException(
          "Header and operation idempotency keys must match"
      );
    }
    if (!operation.context().expectedHeadHash().equals(
        StrictJsonRequest.unquoteEtag(ifMatch)
    )) {
      throw new IllegalArgumentException(
          "If-Match and operation expected_head_hash must match"
      );
    }
    Instant now = Instant.now(self.clock);
    AuditContext auditContext = AccessPrincipalSupport.auditContext(
        authentication, servletRequest, now
    );
    CommitResult result;
    try {
      result = self.commits.commit(
          operation,
          new Ids.RevisionId(StrictJsonRequest.string(
              request, "candidate_revision_id", "commit request"
          )),
          StrictJsonRequest.instant(
              request, "candidate_created_at", "commit request"
          ),
          auditContext
      );
    } catch (CommitRejectedException failure) {
      self.securityStore.appendAuditEvent(AuditEvent.create(
          auditContext,
          requestedNovel,
          AuditAction.COMMIT,
          operation.context().operationId().value(),
          null,
          null,
          AuditResult.REJECTED,
          EditOperationCanonicalMapper.hash(operation),
          failure.preview().candidateHash()
      ));
      throw failure;
    }
    return CommitResponse.create(result);
  }
}
