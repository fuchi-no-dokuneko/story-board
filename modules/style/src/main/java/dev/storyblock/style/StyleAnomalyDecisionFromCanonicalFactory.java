package dev.storyblock.style;

import java.util.Map;
import java.util.Set;

final class StyleAnomalyDecisionFromCanonicalFactory {
    static StyleAnomalyDecision fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, Set.of(
                "can_trigger_rewrite", "confidence", "independent_q99_channels",
                "intentional_shift_adjusted", "localized_micro_window_ids",
                "operational_window_id", "reason", "state",
                "sustaining_window_ids"
        ), "style_anomaly_decision");
        return new StyleAnomalyDecision(
                StyleDecisionState.fromCanonicalName(StyleCanonical.string(
                        value, "state", "style_anomaly_decision"
                )),
                StyleDecisionReason.fromCanonicalName(StyleCanonical.string(
                        value, "reason", "style_anomaly_decision"
                )),
                StyleCalibrationConfidence.fromCanonicalName(StyleCanonical.string(
                        value, "confidence", "style_anomaly_decision"
                )),
                StyleCanonical.string(
                        value, "operational_window_id", "style_anomaly_decision"
                ),
                StyleCanonical.strings(
                        value.get("independent_q99_channels"),
                        "style_anomaly_decision.independent_q99_channels"
                ).stream().map(StyleFeatureChannel::fromCanonicalName).toList(),
                StyleCanonical.strings(
                        value.get("sustaining_window_ids"),
                        "style_anomaly_decision.sustaining_window_ids"
                ),
                StyleCanonical.strings(
                        value.get("localized_micro_window_ids"),
                        "style_anomaly_decision.localized_micro_window_ids"
                ),
                StyleCanonical.bool(
                        value, "intentional_shift_adjusted", "style_anomaly_decision"
                ),
                StyleCanonical.bool(
                        value, "can_trigger_rewrite", "style_anomaly_decision"
                )
        );
    }
}
