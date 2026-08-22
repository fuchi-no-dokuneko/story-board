package dev.storyblock.style;

import java.util.List;
import java.util.Objects;

public final class StyleAnomalyPolicy {
    public StyleAnomalyDecision evaluate(
            StyleWindowScore operational,
            List<StyleWindowScore> nonOverlap,
            List<StyleWindowScore> micro
    ) {
        Objects.requireNonNull(operational, "operational");
        nonOverlap = List.copyOf(nonOverlap);
        micro = List.copyOf(micro);
        if (!operational.window().primaryDecisionEligible()) {
            throw new IllegalArgumentException(
                    "Style decisions require a full operational window"
            );
        }
        validateSupportingWindows(operational, nonOverlap, micro);

        List<String> localized = micro.stream()
                .filter(StyleWindowScore::hasAnyAboveQ95)
                .map(score -> score.window().windowId())
                .toList();
        StyleCalibrationConfidence confidence = operational.profileSelection().confidence();
        if (!operational.profileSelection().calibrationAvailable()
                || confidence == StyleCalibrationConfidence.LOW_CONFIDENCE) {
            return decision(
                    operational,
                    StyleDecisionState.LOW_CONFIDENCE,
                    StyleDecisionReason.INSUFFICIENT_CALIBRATION,
                    confidence,
                    List.of(),
                    localized,
                    false
            );
        }
        if (operational.surfaceOnlyAboveQ95()) {
            return decision(
                    operational,
                    StyleDecisionState.TOPIC_SHIFT_ONLY,
                    StyleDecisionReason.TOKEN_CHANNEL_ONLY,
                    confidence,
                    List.of(),
                    localized,
                    false
            );
        }

        long q95 = operational.independentAboveQ95();
        long q99 = operational.independentAboveQ99();
        List<String> sustaining = nonOverlap.stream()
                .filter(score -> score.profileSelection().confidence()
                        == StyleCalibrationConfidence.CALIBRATED)
                .filter(score -> score.independentAboveQ99() >= 2)
                .map(score -> score.window().windowId())
                .toList();
        StyleDecisionState state;
        StyleDecisionReason reason;
        if (q99 >= 2 && sustaining.size() >= 2) {
            state = StyleDecisionState.REWRITE_CANDIDATE;
            reason = StyleDecisionReason.SUSTAINED_MULTI_CHANNEL_Q99;
        } else if (q95 >= 2) {
            state = StyleDecisionState.WARNING;
            reason = StyleDecisionReason.MULTI_CHANNEL_Q95;
        } else {
            state = StyleDecisionState.NORMAL;
            reason = StyleDecisionReason.WITHIN_CALIBRATED_RANGE;
        }

        boolean adjusted = operational.window().intentionalStyleShiftReason() != null
                && (state == StyleDecisionState.WARNING
                || state == StyleDecisionState.REWRITE_CANDIDATE);
        if (adjusted) {
            state = state == StyleDecisionState.REWRITE_CANDIDATE
                    ? StyleDecisionState.WARNING : StyleDecisionState.NORMAL;
            reason = StyleDecisionReason.INTENTIONAL_STYLE_SHIFT;
        }
        return decision(
                operational,
                state,
                reason,
                confidence,
                sustaining,
                localized,
                adjusted
        );
    }

    private static StyleAnomalyDecision decision(
            StyleWindowScore operational,
            StyleDecisionState state,
            StyleDecisionReason reason,
            StyleCalibrationConfidence confidence,
            List<String> sustaining,
            List<String> localized,
            boolean adjusted
    ) {
        return new StyleAnomalyDecision(
                state,
                reason,
                confidence,
                operational.window().windowId(),
                operational.channels().stream()
                        .filter(score -> score.distance().independentGateEvidence())
                        .filter(StyleCalibratedChannelScore::aboveQ99)
                        .map(score -> score.distance().channel())
                        .sorted(java.util.Comparator.comparing(Enum::ordinal))
                        .toList(),
                sustaining,
                localized,
                adjusted,
                state == StyleDecisionState.REWRITE_CANDIDATE
        );
    }

    private static void validateSupportingWindows(
            StyleWindowScore operational,
            List<StyleWindowScore> nonOverlap,
            List<StyleWindowScore> micro
    ) {
        if (nonOverlap.stream().anyMatch(score ->
                !score.window().sustainmentEligible()
                        || !sameContext(operational, score)
        )) {
            throw new IllegalArgumentException(
                    "Style sustainment inputs must be full non-overlap windows in the same context"
            );
        }
        if (micro.stream().anyMatch(score ->
                !score.window().localizationOnly()
                        || !sameContext(operational, score)
        )) {
            throw new IllegalArgumentException(
                    "Style localization inputs must be micro windows in the same context"
            );
        }
        if (nonOverlap.stream().map(score -> score.window().windowId())
                .distinct().count() != nonOverlap.size()
                || micro.stream().map(score -> score.window().windowId())
                .distinct().count() != micro.size()) {
            throw new IllegalArgumentException(
                    "Style supporting windows must have unique identities"
            );
        }
        for (int first = 0; first < nonOverlap.size(); first++) {
            for (int second = first + 1; second < nonOverlap.size(); second++) {
                if (nonOverlap.get(first).window().overlaps(
                        nonOverlap.get(second).window()
                )) {
                    throw new IllegalArgumentException(
                            "Style sustainment windows must not overlap"
                    );
                }
            }
        }
    }

    private static boolean sameContext(
            StyleWindowScore operational,
            StyleWindowScore supporting
    ) {
        StyleWindow expected = operational.window();
        StyleWindow actual = supporting.window();
        return actual.segment() == expected.segment()
                && actual.requestedStratum().equals(expected.requestedStratum())
                && actual.pov().equals(expected.pov())
                && actual.narrativeMode().equals(expected.narrativeMode())
                && Objects.equals(
                        actual.intentionalStyleShiftReason(),
                        expected.intentionalStyleShiftReason()
                )
                && supporting.profileSelection().selectedStratum().equals(
                        operational.profileSelection().selectedStratum()
                );
    }
}
