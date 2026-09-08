package dev.storyblock.api.http;

import dev.storyblock.domain.Ids;
import dev.storyblock.style.*;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

final class StyleAnalysisControllerCompleteAction {
  static ResponseEntity<Map<String, Object>> complete(StyleAnalysisController self, String jobId, byte[] requestBytes, String ifMatch, String idempotencyKey, Authentication authentication)  {
    Map<String, Object> request = StrictJsonRequest.parseObject(
        requestBytes, "style job result"
    );
    StrictJsonRequest.requireKeys(request, StyleAnalysisController.RESULT_FIELDS, "style job result");
    StyleAnalysisJob job = self.analyses.getJob(new Ids.JobId(jobId));
    AccessPrincipalSupport.requireNovel(
        authentication, job.snapshot().novelId()
    );
    Instant completedAt = StrictJsonRequest.instant(
        request, "completed_at", "style job result"
    );
    if (completedAt.isAfter(Instant.now(self.clock).plusSeconds(30))) {
      throw new IllegalArgumentException(
          "style job result.completed_at cannot be in the future"
      );
    }
    StyleAnalysisTrace trace = StyleAnalysisControllerParseTrace.parseTrace(
        job,
        StrictJsonRequest.object(
            request.get("trace"), "style job result.trace"
        ),
        completedAt
    );
    var result = self.analyses.complete(new StyleAnalysisCompletionCommand(
        job.jobId(),
        StrictJsonRequest.string(
            request, "lease_owner", "style job result"
        ),
        StrictJsonRequest.integer(request, "attempt", "style job result"),
        StrictJsonRequest.unquoteEtag(ifMatch),
        StrictJsonRequest.string(
            request, "snapshot_hash", "style job result"
        ),
        StrictJsonRequest.string(
            request, "profile_version_hash", "style job result"
        ),
        StrictJsonRequest.string(
            request, "analyzer_contract_hash", "style job result"
        ),
        StrictJsonRequest.string(
            request, "window_configuration_hash", "style job result"
        ),
        StyleAnalysisSummary.fromCanonical(StrictJsonRequest.object(
            request.get("summary"), "style job result.summary"
        )),
        StrictJsonRequest.objects(
            request.get("windows"), "style job result.windows"
        ).stream().map(StyleAnalysisWindowFinding::fromCanonical).toList(),
        trace,
        idempotencyKey,
        completedAt
    ));
    return ResponseEntity.status(HttpStatus.OK)
        .eTag(result.job().statusHash())
        .body(Map.of(
            "analysis_id", result.result().analysisId().value(),
            "idempotent_replay", result.idempotentReplay(),
            "job_id", result.job().jobId().value(),
            "result_hash", result.result().resultHash(),
            "status", result.job().status().canonicalName()
        ));
  }
}
