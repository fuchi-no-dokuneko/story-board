package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StyleAnomalyDecision(
        StyleDecisionState state,
        StyleDecisionReason reason,
        StyleCalibrationConfidence confidence,
        String operationalWindowId,
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
                && sustainingWindowIds.size() < 2) {
            throw new IllegalArgumentException(
                    "Rewrite candidate requires two non-overlap sustaining windows"
            );
        }
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("can_trigger_rewrite", canTriggerRewrite);
        value.put("confidence", confidence.canonicalName());
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
