package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleAnalysisWindowFinding(
        int ordinal,
        String windowId,
        List<Ids.BlockId> blockIds,
        StyleDecisionState decisionState,
        StyleCalibrationConfidence confidence,
        boolean canTriggerRewrite,
        Map<String, Object> payload
) {
    static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    static final Set<String> FIELDS = Set.of(
            "ordinal", "window_id", "block_ids", "decision_state", "confidence",
            "can_trigger_rewrite", "payload"
    );

    public StyleAnalysisWindowFinding {
        if (ordinal < 0 || windowId == null || !HASH.matcher(windowId).matches()) {
            throw new IllegalArgumentException("Style analysis window identity is invalid");
        }
        blockIds = List.copyOf(blockIds);
        if (blockIds.isEmpty() || new HashSet<>(blockIds).size() != blockIds.size()) {
            throw new IllegalArgumentException(
                    "Style analysis finding block IDs must be nonempty and unique"
            );
        }
        Objects.requireNonNull(decisionState, "decisionState");
        Objects.requireNonNull(confidence, "confidence");
        if (canTriggerRewrite != (decisionState == StyleDecisionState.REWRITE_CANDIDATE)
                || (confidence == StyleCalibrationConfidence.LOW_CONFIDENCE
                && decisionState != StyleDecisionState.LOW_CONFIDENCE)) {
            throw new IllegalArgumentException(
                    "Style analysis finding rewrite or confidence state is inconsistent"
            );
        }
        payload = CanonicalValues.freezeMap(payload, "style_analysis_window.payload");
    }

    public static StyleAnalysisWindowFinding fromCanonical(Map<String, Object> value) {
        return StyleAnalysisWindowFindingFromCanonicalFactory.fromCanonical(value);
    }

    public Map<String, Object> canonicalValue() {
        return StyleAnalysisWindowFindingCanonicalValueAction.canonicalValue(this);
    }
}
