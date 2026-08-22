package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/v1")
public final class V1ContractController {
    @PostMapping("/novels")
    ResponseEntity<Map<String, Object>> createNovel(@RequestBody JsonNode request) {
        return unavailable();
    }

    @GetMapping("/novels/{novelId}")
    ResponseEntity<Map<String, Object>> getNovel(@PathVariable String novelId) {
        return unavailable();
    }

    @GetMapping("/novels/{novelId}/revisions/{revisionId}")
    ResponseEntity<Map<String, Object>> getRevision(
            @PathVariable String novelId,
            @PathVariable String revisionId
    ) {
        return unavailable();
    }

    @PostMapping("/novels/{novelId}/edit-previews")
    ResponseEntity<Map<String, Object>> previewEdit(
            @PathVariable String novelId,
            @RequestBody JsonNode request
    ) {
        return unavailable();
    }

    @PostMapping("/novels/{novelId}/undo-previews")
    ResponseEntity<Map<String, Object>> previewUndo(
            @PathVariable String novelId,
            @RequestBody JsonNode request
    ) {
        return unavailable();
    }

    @PostMapping("/rewrite-proposals")
    ResponseEntity<Map<String, Object>> startRewriteProposal(
            @RequestBody JsonNode request,
            Authentication authentication
    ) {
        JsonNode novelId = request.get("novel_id");
        if (novelId != null && novelId.isString()) {
            AccessPrincipalSupport.requireNovel(
                    authentication, new Ids.NovelId(novelId.stringValue())
            );
        }
        return unavailable();
    }

    @GetMapping("/rewrite-proposals/{proposalId}")
    ResponseEntity<Map<String, Object>> getRewriteProposal(
            @PathVariable String proposalId
    ) {
        return unavailable();
    }

    private static ResponseEntity<Map<String, Object>> unavailable() {
        throw ApiFailureException.unavailable(
                "The route contract is active; its owning application service is not installed."
        );
    }
}
