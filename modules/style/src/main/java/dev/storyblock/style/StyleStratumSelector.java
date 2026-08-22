package dev.storyblock.style;

import java.util.Objects;
import java.util.Optional;

public final class StyleStratumSelector {
    public StyleProfileSelection select(
            StyleStratum requested,
            StyleCalibrationProfile profile
    ) {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(profile, "profile");
        Optional<StyleStratumCalibration> exact = profile.find(requested);
        if (!requested.speakerSpecific()) {
            return selection(requested, requested, exact, StyleSelectionReason.EXACT);
        }
        if (exact.isPresent()
                && exact.get().confidence() == StyleCalibrationConfidence.CALIBRATED) {
            return selection(requested, requested, exact, StyleSelectionReason.EXACT);
        }
        StyleStratum fallback = StyleStratum.dialogue();
        Optional<StyleStratumCalibration> global = profile.find(fallback);
        if (global.isPresent()) {
            return selection(
                    requested, fallback, global, StyleSelectionReason.SPEAKER_FALLBACK
            );
        }
        if (exact.isPresent()) {
            return selection(requested, requested, exact, StyleSelectionReason.EXACT);
        }
        return selection(
                requested,
                fallback,
                Optional.empty(),
                StyleSelectionReason.UNAVAILABLE
        );
    }

    private static StyleProfileSelection selection(
            StyleStratum requested,
            StyleStratum selected,
            Optional<StyleStratumCalibration> calibration,
            StyleSelectionReason reason
    ) {
        return new StyleProfileSelection(
                requested,
                selected,
                reason,
                calibration.isPresent(),
                calibration.map(StyleStratumCalibration::confidence)
                        .orElse(StyleCalibrationConfidence.LOW_CONFIDENCE)
        );
    }
}
