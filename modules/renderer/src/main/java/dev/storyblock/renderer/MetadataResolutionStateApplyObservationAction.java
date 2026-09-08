package dev.storyblock.renderer;

import dev.storyblock.domain.MetadataValueState;
import java.util.Map;

final class MetadataResolutionStateApplyObservationAction {
    static void applyObservation(MetadataResolutionState self, String field, Object raw, String path)  {
        if (!(raw instanceof Map<?, ?> observation)
                || !(observation.get("mode") instanceof String canonicalMode)) {
            throw new IllegalArgumentException(path + " must declare a metadata mode");
        }

        MetadataValueState mode = MetadataValueState.fromCanonicalName(canonicalMode);
        switch (mode) {
            case EXPLICIT -> self.values.put(field, MetadataResolutionStateExplicitValue.explicitValue(observation, path));
            case INHERITED -> {
                // Retain the resolved value, including unknown or not-applicable.
            }
            case UNKNOWN -> self.values.put(field, MetadataResolutionState.UNKNOWN);
            case NOT_APPLICABLE -> self.values.put(field, MetadataResolutionState.NOT_APPLICABLE);
        }
    }
}
