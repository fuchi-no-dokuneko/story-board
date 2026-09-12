package dev.storyblock.api.http;

import dev.storyblock.application.PreviewResponse;
import dev.storyblock.domain.EditContext;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class PreviewControllerUndoAction {
    static ResponseEntity<Map<String, Object>> undo(PreviewController self, String novelId, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication)  {
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
        PreviewResponse result = self.service(requestedNovel).preview(
                self.revisions.getRevision(
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
}
