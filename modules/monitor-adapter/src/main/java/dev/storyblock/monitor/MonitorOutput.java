package dev.storyblock.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public sealed interface MonitorOutput permits MonitorFinding, MonitorProposedOperation {
    MonitorOutputKind kind();

    List<MonitorEvidence> evidence();

    Map<String, Object> canonicalValue();

    static MonitorOutput fromCanonical(Map<String, Object> value) {
        Object rawKind = value.get("kind");
        if (!(rawKind instanceof String canonicalKind)) {
            throw new IllegalArgumentException("Monitor output.kind must be a string");
        }
        return switch (MonitorOutputKind.fromCanonicalName(canonicalKind)) {
            case FINDING -> MonitorFinding.fromCanonical(value);
            case PROPOSED_OPERATION -> MonitorProposedOperation.fromCanonical(value);
        };
    }

    static List<MonitorEvidence> parseEvidence(Object value) {
        if (!(value instanceof List<?> entries) || entries.isEmpty() || entries.size() > 16) {
            throw new IllegalArgumentException("Monitor output requires 1 to 16 evidence spans");
        }
        List<MonitorEvidence> result = new ArrayList<>(entries.size());
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> raw)) {
                throw new IllegalArgumentException("Monitor evidence must be an object");
            }
            java.util.LinkedHashMap<String, Object> typed = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> field : raw.entrySet()) {
                if (!(field.getKey() instanceof String key)) {
                    throw new IllegalArgumentException("Monitor evidence has a non-string key");
                }
                typed.put(key, field.getValue());
            }
            result.add(MonitorEvidence.fromCanonical(typed));
        }
        return List.copyOf(result);
    }
}
