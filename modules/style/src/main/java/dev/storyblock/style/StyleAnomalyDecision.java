package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record StyleAnomalyDecision(
        StyleDecisionState state,
        StyleDecisionReason reason,
        StyleCalibrationConfidence confidence,
        String operationalWindowId,
        List<StyleFeatureChannel> independentQ99Channels,
        List<String> sustainingWindowIds,
        List<String> localizedMicroWindowIds,
        boolean intentionalShiftAdjusted,
        boolean canTriggerRewrite
) {
    public StyleAnomalyDecision {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(confidence, "confidence");
        if (operationalWindowId == null
                || !operationalWindowId.matches("sha256:[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Style decision window ID is invalid");
        }
        independentQ99Channels = List.copyOf(independentQ99Channels);
        if (Set.copyOf(independentQ99Channels).size()
                != independentQ99Channels.size()
                || independentQ99Channels.stream().anyMatch(channel -> !channel.required())
                || !independentQ99Channels.stream()
                        .sorted(java.util.Comparator.comparing(Enum::ordinal))
                        .toList().equals(independentQ99Channels)) {
            throw new IllegalArgumentException(
                    "Style decision independent q99 channels are invalid"
            );
        }
        sustainingWindowIds = uniqueHashes(sustainingWindowIds, "sustaining");
        localizedMicroWindowIds = uniqueHashes(localizedMicroWindowIds, "micro");
        if (canTriggerRewrite != (state == StyleDecisionState.REWRITE_CANDIDATE)
                || (confidence == StyleCalibrationConfidence.LOW_CONFIDENCE
                && canTriggerRewrite)
                || (state == StyleDecisionState.TOPIC_SHIFT_ONLY && canTriggerRewrite)) {
            throw new IllegalArgumentException(
                    "Style rewrite eligibility does not match decision state"
            );
        }
        if (state == StyleDecisionState.REWRITE_CANDIDATE
                && (reason != StyleDecisionReason.SUSTAINED_MULTI_CHANNEL_Q99
                || confidence != StyleCalibrationConfidence.CALIBRATED
                || independentQ99Channels.size() < 2
                || sustainingWindowIds.size() < 2
                || intentionalShiftAdjusted)) {
            throw new IllegalArgumentException(
                    "Rewrite candidate lacks calibrated sustained q99 evidence"
            );
        }
    }

    public static StyleAnomalyDecision fromCanonical(Map<String, Object> value) {
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

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("can_trigger_rewrite", canTriggerRewrite);
        value.put("confidence", confidence.canonicalName());
        value.put("independent_q99_channels", independentQ99Channels.stream()
                .map(StyleFeatureChannel::canonicalName).toList());
        value.put("intentional_shift_adjusted", intentionalShiftAdjusted);
        value.put("localized_micro_window_ids", localizedMicroWindowIds);
        value.put("operational_window_id", operationalWindowId);
        value.put("reason", reason.canonicalName());
        value.put("state", state.canonicalName());
        value.put("sustaining_window_ids", sustainingWindowIds);
        return CanonicalValues.freezeMap(value, "style_anomaly_decision");
    }

    private static List<String> uniqueHashes(List<String> values, String field) {
        values = List.copyOf(values);
        if (new LinkedHashSet<>(values).size() != values.size()
                || values.stream().anyMatch(value ->
                        value == null || !value.matches("sha256:[0-9a-f]{64}")
                )) {
            throw new IllegalArgumentException(
                    "Style decision " + field + " window IDs are invalid"
            );
        }
        return values;
    }
}
