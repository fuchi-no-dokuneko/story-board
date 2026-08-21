package dev.storyblock.api.http;

import dev.storyblock.application.CommitRejectedException;
import dev.storyblock.application.CommitService;
import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AccessKeyStore;
import dev.storyblock.security.AuditAction;
import dev.storyblock.security.AuditContext;
import dev.storyblock.security.AuditEvent;
import dev.storyblock.security.AuditResult;
import dev.storyblock.security.CrossNovelAccessException;
import dev.storyblock.storage.CommitResult;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public final class CommitController {
    private final CommitService commits;
    private final AccessKeyStore securityStore;
    private final Clock clock;

    public CommitController(
            CommitService commits,
            AccessKeyStore securityStore,
            Clock clock
    ) {
        this.commits = java.util.Objects.requireNonNull(commits, "commits");
        this.securityStore = java.util.Objects.requireNonNull(
                securityStore, "securityStore"
        );
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @PostMapping("/novels/{novelId}/commits")
    ResponseEntity<Map<String, Object>> commit(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
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
        Instant now = Instant.now(clock);
        AuditContext auditContext = AccessPrincipalSupport.auditContext(
                authentication, servletRequest, now
        );
        CommitResult result;
        try {
            result = commits.commit(
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
            securityStore.appendAuditEvent(AuditEvent.create(
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
        HttpStatus status = result.idempotentReplay()
                ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .header(HttpHeaders.ETAG, "\"" + result.revision().contentHash() + "\"")
                .body(Map.of(
                        "revision_id", result.revision().revisionId().value(),
                        "sequence", result.revision().sequence(),
                        "content_hash", result.revision().contentHash(),
                        "operation_id", result.operationId().value(),
                        "idempotent_replay", result.idempotentReplay()
                ));
    }
}
