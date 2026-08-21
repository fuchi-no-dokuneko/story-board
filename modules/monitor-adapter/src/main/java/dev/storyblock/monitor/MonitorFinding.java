package dev.storyblock.monitor;

import dev.storyblock.domain.CanonicalValues;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record MonitorFinding(
        String code,
        String severity,
        String message,
        List<MonitorEvidence> evidence
) implements MonitorOutput {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{2,63}");
    private static final Set<String> SEVERITIES = Set.of("error", "warning", "info");
    private static final Set<String> FIELDS = Set.of(
            "kind", "code", "severity", "message", "evidence"
    );

    public MonitorFinding {
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Monitor finding code is invalid");
        }
        if (!SEVERITIES.contains(severity)) {
            throw new IllegalArgumentException("Monitor finding severity is invalid");
        }
        if (message == null || message.isBlank() || message.length() > 2_000) {
            throw new IllegalArgumentException("Monitor finding message is invalid");
        }
        evidence = List.copyOf(evidence);
        if (evidence.isEmpty() || evidence.size() > 16) {
            throw new IllegalArgumentException("Monitor finding requires 1 to 16 evidence spans");
        }
    }

    static MonitorFinding fromCanonical(Map<String, Object> value) {
        if (!value.keySet().equals(FIELDS)) {
            throw new IllegalArgumentException("Monitor finding fields are invalid");
        }
        return new MonitorFinding(
                string(value, "code"),
                string(value, "severity"),
                string(value, "message"),
                MonitorOutput.parseEvidence(value.get("evidence"))
        );
    }

    @Override
    public MonitorOutputKind kind() {
        return MonitorOutputKind.FINDING;
    }

    @Override
    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.of(
                "code", code,
                "evidence", evidence.stream().map(MonitorEvidence::canonicalValue).toList(),
                "kind", kind().canonicalName(),
                "message", message,
                "severity", severity
        ), "monitor_finding");
    }

    private static String string(Map<String, Object> value, String field) {
        Object raw = value.get(field);
        if (!(raw instanceof String text)) {
            throw new IllegalArgumentException("Monitor finding." + field + " must be a string");
        }
        return text;
    }
}
