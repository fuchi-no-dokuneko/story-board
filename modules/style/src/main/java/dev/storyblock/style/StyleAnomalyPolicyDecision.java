package dev.storyblock.style;

import java.util.List;

final class StyleAnomalyPolicyDecision {
    static StyleAnomalyDecision decision(
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
}
