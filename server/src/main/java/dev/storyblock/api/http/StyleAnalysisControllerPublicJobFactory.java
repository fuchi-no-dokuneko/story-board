package dev.storyblock.api.http;

import dev.storyblock.style.StyleAnalysisJob;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleAnalysisControllerPublicJobFactory {
    static Map<String, Object> publicJob(StyleAnalysisJob job)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", job.analysisId().value());
        value.put("attempt", job.attempt());
        value.put("created_at", job.createdAt().toString());
        value.put("failure_code", job.failureCode());
        value.put("job_id", job.jobId().value());
        value.put("kind", "style-analysis");
        value.put("max_attempts", job.maxAttempts());
        value.put("novel_id", job.snapshot().novelId().value());
        value.put("result_artifact_id", job.resultArtifactId() == null
                ? null : job.resultArtifactId().value());
        value.put("result_hash", job.resultHash());
        value.put("retention_until", job.retentionUntil().toString());
        value.put("revision_id", job.snapshot().revisionId().value());
        value.put("status", job.status().canonicalName());
        value.put("updated_at", job.updatedAt().toString());
        return value;
    }
}
