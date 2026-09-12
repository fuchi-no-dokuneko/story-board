package dev.storyblock.api.http;

import dev.storyblock.application.PreviewService;
import dev.storyblock.domain.Ids;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.util.Map;
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
    final SqliteRevisionStore revisions;

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
        return PreviewControllerPreviewAction.preview(this, novelId, requestBytes, ifMatch, idempotencyKey, authentication);
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
        return PreviewControllerUndoAction.undo(this, novelId, requestBytes, ifMatch, idempotencyKey, authentication);
    }

    PreviewService service(Ids.NovelId novelId) {
        return new PreviewService(revisionId -> revisions.getRevision(
                novelId, revisionId
        ).manifest());
    }

}
