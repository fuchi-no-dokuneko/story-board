package dev.storyblock.domain;

import java.util.Map;
import java.util.Objects;

public record LocalObservation(MetadataValueState state, Object value) {
    public LocalObservation {
        Objects.requireNonNull(state, "state");
        if (state == MetadataValueState.EXPLICIT) {
            if (value == null) {
                throw new IllegalArgumentException("Explicit metadata requires a value");
            }
            value = CanonicalValues.freeze(value, "local_observation.value");
        } else if (value != null) {
            throw new IllegalArgumentException(state.canonicalName() + " metadata cannot carry a value");
        }
    }

    public static LocalObservation explicit(Object value) {
        return new LocalObservation(MetadataValueState.EXPLICIT, value);
    }

    public static LocalObservation inherited() {
        return new LocalObservation(MetadataValueState.INHERITED, null);
    }

    public static LocalObservation unknown() {
        return new LocalObservation(MetadataValueState.UNKNOWN, null);
    }

    public static LocalObservation notApplicable() {
        return new LocalObservation(MetadataValueState.NOT_APPLICABLE, null);
    }

    public Map<String, Object> canonicalValue() {
        if (value == null) {
            return Map.of("mode", state.canonicalName());
        }
        return Map.of("mode", state.canonicalName(), "value", value);
    }
}
