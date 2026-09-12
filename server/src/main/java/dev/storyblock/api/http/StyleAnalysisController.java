package dev.storyblock.api.http;

import dev.storyblock.application.StyleAnalysisService;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.StyleAnalysisJob;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public final class StyleAnalysisController {
  final StyleAnalysisService analyses;
  final Clock clock;

  public StyleAnalysisController(StyleAnalysisService analyses, Clock clock) {
    this.analyses = java.util.Objects.requireNonNull(analyses, "analyses");
    this.clock = java.util.Objects.requireNonNull(clock, "clock");
  }

  @PostMapping("/novels/{novelId}/style-analyses")
  ResponseEntity<Map<String, Object>> create(
      @PathVariable String novelId,
      @RequestBody byte[] requestBytes,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
      @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
      Authentication authentication,
      HttpServletRequest servletRequest
  ) {
    return StyleAnalysisControllerCreateAction.create(this, novelId, requestBytes, ifMatch, idempotencyKey, authentication, servletRequest);
  }

  @GetMapping("/style-analyses/{analysisId}")
  ResponseEntity<Map<String, Object>> getAnalysis(@PathVariable String analysisId) {
    return StyleAnalysisControllerGetAnalysisAction.getAnalysis(this, analysisId);
  }

  @GetMapping("/style-analyses/{analysisId}/windows")
  ResponseEntity<Map<String, Object>> windows(
      @PathVariable String analysisId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "50") int limit
  ) {
    return ResponseEntity.ok(analyses.windows(
        new Ids.StyleAnalysisId(analysisId), cursor, limit
    ).canonicalValue());
  }

  @PostMapping("/internal/jobs/claims")
  ResponseEntity<Map<String, Object>> claim(
      @RequestBody byte[] requestBytes,
      @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
      Authentication authentication
  ) {
    return StyleAnalysisControllerClaimAction.claim(this, requestBytes, idempotencyKey, authentication);
  }

  @PostMapping("/internal/jobs/{jobId}/results")
  ResponseEntity<Map<String, Object>> complete(
      @PathVariable String jobId,
      @RequestBody byte[] requestBytes,
      @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
      @RequestHeader(MutationPreconditionFilter.IDEMPOTENCY_KEY) String idempotencyKey,
      Authentication authentication
  ) {
    return StyleAnalysisCompletion.complete(this, jobId, requestBytes, ifMatch, idempotencyKey, authentication);
  }

  static Map<String, Object> publicJob(StyleAnalysisJob job) {
    return StyleAnalysisControllerPublicJobFactory.publicJob(job);
  }

}
