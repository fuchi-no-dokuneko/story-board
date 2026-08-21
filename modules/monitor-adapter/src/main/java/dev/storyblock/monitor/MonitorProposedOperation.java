package dev.storyblock.monitor;

import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.EditOperation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record MonitorProposedOperation(
        EditOperation operation,
        List<MonitorEvidence> evidence
) implements MonitorOutput {
    private static final Set<String> FIELDS = Set.of("kind", "operation", "evidence");

    public MonitorProposedOperation {
        java.util.Objects.requireNonNull(operation, "operation");
        evidence = List.copyOf(evidence);
        if (evidence.isEmpty() || evidence.size() > 16) {
            throw new IllegalArgumentException(
                    "Monitor proposed operation requires 1 to 16 evidence spans"
            );
        }
    }

    static MonitorProposedOperation fromCanonical(Map<String, Object> value) {
        if (!value.keySet().equals(FIELDS)) {
            throw new IllegalArgumentException("Monitor proposed-operation fields are invalid");
        }
        return new MonitorProposedOperation(
                EditOperationCanonicalMapper.fromCanonical(object(
                        value.get("operation"), "monitor proposed operation.operation"
                )),
                MonitorOutput.parseEvidence(value.get("evidence"))
        );
    }

    @Override
    public MonitorOutputKind kind() {
        return MonitorOutputKind.PROPOSED_OPERATION;
    }

    @Override
    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("kind", kind().canonicalName());
        value.put("operation", EditOperationCanonicalMapper.toCanonical(operation));
        value.put("evidence", evidence.stream().map(MonitorEvidence::canonicalValue).toList());
        return CanonicalValues.freezeMap(value, "monitor_proposed_operation");
    }

    private static Map<String, Object> object(Object value, String path) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        Map<String, Object> typed = new LinkedHashMap<>();
        for (Map.Entry<?, ?> field : raw.entrySet()) {
            if (!(field.getKey() instanceof String key)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
            typed.put(key, field.getValue());
        }
        return typed;
    }
}
