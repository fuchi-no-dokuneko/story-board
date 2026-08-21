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
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping("/novels/{novelId}/renders")
    ResponseEntity<Map<String, Object>> render(
            @PathVariable String novelId,
            @RequestBody JsonNode request
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

    @PostMapping("/novels/{novelId}/detector-runs")
    ResponseEntity<Map<String, Object>> runDetector(
            @PathVariable String novelId,
            @RequestBody JsonNode request
    ) {
        return unavailable();
    }

    @PostMapping("/novels/{novelId}/style-analyses")
    ResponseEntity<Map<String, Object>> startStyleAnalysis(
            @PathVariable String novelId,
            @RequestBody JsonNode request
    ) {
        return unavailable();
    }

    @GetMapping("/style-analyses/{analysisId}")
    ResponseEntity<Map<String, Object>> getStyleAnalysis(
            @PathVariable String analysisId,
            @RequestParam(required = false) String cursor
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

    @PostMapping("/style-profiles")
    ResponseEntity<Map<String, Object>> createStyleProfile(
            @RequestBody JsonNode request
    ) {
        return unavailable();
    }

    @PostMapping("/style-profiles/{profileId}/versions")
    ResponseEntity<Map<String, Object>> createStyleProfileVersion(
            @PathVariable String profileId,
            @RequestBody JsonNode request
    ) {
        return unavailable();
    }

    @PostMapping("/internal/jobs/claims")
    ResponseEntity<Map<String, Object>> claimJob(@RequestBody JsonNode request) {
        return unavailable();
    }

    @PostMapping("/internal/jobs/{jobId}/results")
    ResponseEntity<Map<String, Object>> submitJobResult(
            @PathVariable String jobId,
            @RequestBody JsonNode request
    ) {
        return unavailable();
    }

    private static ResponseEntity<Map<String, Object>> unavailable() {
        throw ApiFailureException.unavailable(
                "The route contract is active; its owning application service is not installed."
        );
    }
}
