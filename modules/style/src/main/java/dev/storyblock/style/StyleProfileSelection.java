package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record StyleProfileSelection(
        StyleStratum requestedStratum,
        StyleStratum selectedStratum,
        StyleSelectionReason reason,
        boolean calibrationAvailable,
        StyleCalibrationConfidence confidence
) {
    public StyleProfileSelection {
        Objects.requireNonNull(requestedStratum, "requestedStratum");
        Objects.requireNonNull(selectedStratum, "selectedStratum");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(confidence, "confidence");
        if (reason == StyleSelectionReason.SPEAKER_FALLBACK
                && (!requestedStratum.speakerSpecific()
                || !selectedStratum.equals(StyleStratum.dialogue()))) {
            throw new IllegalArgumentException("Style speaker fallback selection is invalid");
        }
        if (!calibrationAvailable && confidence != StyleCalibrationConfidence.LOW_CONFIDENCE) {
            throw new IllegalArgumentException(
                    "Unavailable style calibration must be LOW_CONFIDENCE"
            );
        }
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("calibration_available", calibrationAvailable);
        value.put("confidence", confidence.canonicalName());
        value.put("reason", reason.canonicalName());
        value.put("requested_stratum", requestedStratum.canonicalValue());
        value.put("selected_stratum", selectedStratum.canonicalValue());
        return CanonicalValues.freezeMap(value, "style_profile_selection");
    }
}
