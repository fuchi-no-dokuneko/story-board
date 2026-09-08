package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleAnalysisJobStatusValueAction {
    static Map<String, Object> statusValue(StyleAnalysisJob self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", self.analysisId().value());
        value.put("attempt", self.attempt());
        value.put("created_at", self.createdAt().toString());
        value.put("created_by", self.auditContext().actorId());
        value.put("failure_code", self.failureCode());
        value.put("job_id", self.jobId().value());
        value.put("lease_owner", self.leaseOwner());
        value.put("lease_until", self.leaseUntil() == null ? null : self.leaseUntil().toString());
        value.put("max_attempts", self.maxAttempts());
        value.put("novel_id", self.snapshot().novelId().value());
        value.put("profile_version_hash", self.snapshot().profileVersionHash());
        value.put("result_artifact_id", self.resultArtifactId() == null
                ? null : self.resultArtifactId().value());
        value.put("result_hash", self.resultHash());
        value.put("retention_until", self.retentionUntil().toString());
        value.put("revision_hash", self.snapshot().revisionHash());
        value.put("revision_id", self.snapshot().revisionId().value());
        value.put("snapshot_hash", self.snapshot().snapshotHash());
        value.put("status", self.status().canonicalName());
        value.put("updated_at", self.updatedAt().toString());
        return CanonicalValues.freezeMap(value, "style_analysis_job");
    }
}
