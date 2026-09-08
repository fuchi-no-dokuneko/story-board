package dev.storyblock.api.http;

import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleMaskingLexicon;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class StyleAnalysisControllerCreateAction {
  static ResponseEntity<Map<String, Object>> create(StyleAnalysisController self, String novelId, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication, HttpServletRequest servletRequest)  {
    Ids.NovelId requestedNovel = new Ids.NovelId(novelId);
    AccessPrincipalSupport.requireNovel(authentication, requestedNovel);
    Map<String, Object> request = StrictJsonRequest.parseObject(
        requestBytes, "style analysis request"
    );
    StyleAnalysisControllerRequireFields.requireFields(
        request,
        Set.of("revision_id", "profile_id", "profile_version_id"),
        StyleAnalysisFields.CREATE_FIELDS,
        "style analysis request"
    );
    StyleMaskingLexicon lexicon = request.containsKey("masking_lexicon")
        ? StyleMaskingLexicon.fromCanonical(StrictJsonRequest.object(
            request.get("masking_lexicon"),
            "style analysis request.masking_lexicon"
        ))
        : StyleMaskingLexicon.empty();
    int maxAttempts = request.containsKey("max_attempts")
        ? StrictJsonRequest.integer(
            request, "max_attempts", "style analysis request"
        ) : StyleAnalysisService.DEFAULT_MAX_ATTEMPTS;
    int retentionDays = request.containsKey("retention_days")
        ? StrictJsonRequest.integer(
            request, "retention_days", "style analysis request"
        ) : Math.toIntExact(StyleAnalysisService.DEFAULT_RETENTION.toDays());
    Instant now = Instant.now(self.clock);
    var result = self.analyses.request(
        requestedNovel,
        new Ids.RevisionId(StrictJsonRequest.string(
            request, "revision_id", "style analysis request"
        )),
        StrictJsonRequest.unquoteEtag(ifMatch),
        new Ids.StyleProfileId(StrictJsonRequest.string(
            request, "profile_id", "style analysis request"
        )),
        new Ids.StyleProfileVersionId(StrictJsonRequest.string(
            request, "profile_version_id", "style analysis request"
        )),
        StyleAnalysisControllerOptionalBlockId.optionalBlockId(request.get("from_block_id"), "from_block_id"),
        StyleAnalysisControllerOptionalBlockId.optionalBlockId(request.get("to_block_id"), "to_block_id"),
        lexicon,
        maxAttempts,
        Duration.ofDays(retentionDays),
        idempotencyKey,
        AccessPrincipalSupport.auditContext(
            authentication, servletRequest, now
        )
    );
    StyleAnalysisJob job = result.job();
    String statusUri = "/v1/jobs/" + job.jobId().value();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("analysis_id", job.analysisId().value());
    body.put("analysis_uri", "/v1/style-analyses/" + job.analysisId().value());
    body.put("idempotent_replay", result.idempotentReplay());
    body.put("job_id", job.jobId().value());
    body.put("status", job.status().canonicalName());
    body.put("status_uri", statusUri);
    return ResponseEntity.accepted()
        .location(URI.create(statusUri))
        .eTag(job.statusHash())
        .body(body);
  }
}
