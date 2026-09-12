package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleAnalysisLeaseCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleAnalysisLease self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", self.analysisId().value());
        value.put("attempt", self.attempt());
        value.put("idempotent_replay", self.idempotentReplay());
        value.put("job_id", self.jobId().value());
        value.put("lease_owner", self.leaseOwner());
        value.put("lease_until", self.leaseUntil().toString());
        value.put("retention_until", self.retentionUntil().toString());
        value.put("snapshot", self.snapshot().canonicalValue());
        value.put("status_hash", self.claimedStatusHash());
        return CanonicalValues.freezeMap(value, "style_analysis_lease");
    }
}
