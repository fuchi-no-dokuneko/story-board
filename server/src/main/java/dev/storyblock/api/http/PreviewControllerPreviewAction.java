package dev.storyblock.api.http;

import dev.storyblock.application.PreviewResponse;
import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class PreviewControllerPreviewAction {
    static ResponseEntity<Map<String, Object>> preview(PreviewController self, String novelId, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication)  {
        Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
        AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
        Map<String, Object> request = PreviewControllerRequest.request(requestBytes, "edit preview request");
        EditOperation operation = EditOperationCanonicalMapper.fromCanonical(
                StrictJsonRequest.object(
                        request.get("operation"), "edit preview request.operation"
                )
        );
        PreviewControllerRequireContext.requireContext(operation.context(), requestedNovel, ifMatch, idempotencyKey);
        PreviewResponse result = self.service(requestedNovel).preview(
                self.revisions.getRevision(
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
}
