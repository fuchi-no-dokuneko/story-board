package dev.storyblock.api.http;

import dev.storyblock.application.PreviewResponse;
import dev.storyblock.application.PreviewService;
import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/novels/{novelId}")
public final class PreviewController {
    private final SqliteRevisionStore revisions;

    public PreviewController(SqliteRevisionStore revisions) {
        this.revisions = java.util.Objects.requireNonNull(revisions, "revisions");
    }

    @PostMapping("/edit-previews")
    ResponseEntity<Map<String, Object>> preview(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY)
            String idempotencyKey,
            Authentication authentication
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = request(requestBytes, "edit preview request");
        EditOperation operation = EditOperationCanonicalMapper.fromCanonical(
                StrictJsonRequest.object(
                        request.get("operation"), "edit preview request.operation"
                )
        );
        requireContext(operation.context(), requestedNovel, ifMatch, idempotencyKey);
        PreviewResponse result = service(requestedNovel).preview(
                revisions.getRevision(
                        requestedNovel, operation.context().baseRevisionId()
                ).manifest(),
                operation,
                new Ids.RevisionId(StrictJsonRequest.string(
                        request, "candidate_revision_id", "edit preview request"
                )),
                StrictJsonRequest.instant(
                        request, "candidate_created_at", "edit preview request"
                )
        );
        return ResponseEntity.ok().eTag(result.candidateHash())
                .body(result.contractFields());
    }

    @PostMapping("/undo-previews")
    ResponseEntity<Map<String, Object>> undo(
            @PathVariable String novelId,
            @RequestBody byte[] requestBytes,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY)
            String idempotencyKey,
            Authentication authentication
    ) {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = StrictJsonRequest.parseObject(
                requestBytes, "undo preview request"
        );
        StrictJsonRequest.requireKeys(request, Set.of(
                "base_revision_id", "restore_revision_id", "expected_restore_hash",
                "candidate_revision_id", "candidate_created_at"
        ), "undo preview request");
        EditContext context = new EditContext(
                Ids.OperationId.create(),
                idempotencyKey,
                requestedNovel,
                new Ids.RevisionId(StrictJsonRequest.string(
                        request, "base_revision_id", "undo preview request"
                )),
                StrictJsonRequest.unquoteEtag(ifMatch)
        );
        EditOperation operation = new EditOperation.RestoreRevisionContent(
                context,
                new Ids.RevisionId(StrictJsonRequest.string(
                        request, "restore_revision_id", "undo preview request"
                )),
                StrictJsonRequest.string(
                        request, "expected_restore_hash", "undo preview request"
                )
        );
        PreviewResponse result = service(requestedNovel).preview(
                revisions.getRevision(
                        requestedNovel, context.baseRevisionId()
                ).manifest(),
                operation,
                new Ids.RevisionId(StrictJsonRequest.string(
                        request, "candidate_revision_id", "undo preview request"
                )),
                StrictJsonRequest.instant(
                        request, "candidate_created_at", "undo preview request"
                )
        );
        return ResponseEntity.ok().eTag(result.candidateHash())
                .body(result.contractFields());
    }

    private PreviewService service(Ids.NovelId novelId) {
        return new PreviewService(revisionId -> revisions.getRevision(
                novelId, revisionId
        ).manifest());
    }

    private static Map<String, Object> request(byte[] bytes, String path) {
        Map<String, Object> request = StrictJsonRequest.parseObject(bytes, path);
        StrictJsonRequest.requireKeys(request, Set.of(
                "operation", "candidate_revision_id", "candidate_created_at"
        ), path);
        return request;
    }

    private static void requireContext(
            EditContext context,
            Ids.NovelId novelId,
            String ifMatch,
            String idempotencyKey
    ) {
        if (!context.novelId().equals(novelId)
                || !context.idempotencyKey().equals(idempotencyKey)
                || !context.expectedHeadHash().equals(
                        StrictJsonRequest.unquoteEtag(ifMatch)
                )) {
            throw new IllegalArgumentException(
                    "Preview headers and operation context do not match"
            );
        }
    }
}
