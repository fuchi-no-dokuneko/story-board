package dev.storyblock.monitor;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record MonitorSubmissionResult(
        MonitorRunStatus status,
        boolean idempotentReplay
) {
    public MonitorSubmissionResult {
        Objects.requireNonNull(status, "status");
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>(status.canonicalValue());
        value.put("idempotent_replay", idempotentReplay);
        return CanonicalValues.freezeMap(value, "monitor_submission_result");
    }
}
