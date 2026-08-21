package dev.storyblock.monitor;

import dev.storyblock.domain.CanonicalValues;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record MonitorRunStatus(
        StoredMonitorRun run,
        MonitorRunState state,
        List<MonitorStaleReason> staleReasons
) {
    public MonitorRunStatus {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(state, "state");
        staleReasons = List.copyOf(staleReasons);
        if (new java.util.HashSet<>(staleReasons).size() != staleReasons.size()) {
            throw new IllegalArgumentException("Monitor stale reasons must be unique");
        }
        if ((state == MonitorRunState.CURRENT) != staleReasons.isEmpty()) {
            throw new IllegalArgumentException("Monitor state and stale reasons disagree");
        }
    }

    public static MonitorRunStatus current(StoredMonitorRun run) {
        return new MonitorRunStatus(run, MonitorRunState.CURRENT, List.of());
    }

    public static MonitorRunStatus stale(
            StoredMonitorRun run,
            Set<MonitorStaleReason> reasons
    ) {
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("A stale monitor run requires a reason");
        }
        EnumSet<MonitorStaleReason> ordered = EnumSet.copyOf(reasons);
        return new MonitorRunStatus(run, MonitorRunState.STALE, List.copyOf(ordered));
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>(run.canonicalValue());
        value.put("state", state.canonicalName());
        value.put("stale_reasons", staleReasons.stream()
                .map(MonitorStaleReason::canonicalName).toList());
        value.put("rebase_allowed", false);
        return CanonicalValues.freezeMap(value, "monitor_run_status");
    }
}
