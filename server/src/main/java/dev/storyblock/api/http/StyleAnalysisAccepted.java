package dev.storyblock.api.http;
import dev.storyblock.style.StyleAnalysisJob;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;

final class StyleAnalysisAccepted {
  static ResponseEntity<Map<String, Object>> response(dev.storyblock.style.StyleAnalysisJobSaveResult result) {
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
        .body(body);  }
}
