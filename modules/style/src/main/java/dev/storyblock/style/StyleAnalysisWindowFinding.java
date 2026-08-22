package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Set<String> FIELDS = Set.of(
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
        StyleCanonical.requireKeys(value, FIELDS, "style_analysis_window");
        Object rawIds = value.get("block_ids");
        if (!(rawIds instanceof List<?> values)) {
            throw new IllegalArgumentException(
                    "style_analysis_window.block_ids must be an array"
            );
        }
        List<Ids.BlockId> blockIds = values.stream().map(entry -> {
            if (!(entry instanceof String text)) {
                throw new IllegalArgumentException(
                        "style_analysis_window.block_ids must contain strings"
                );
            }
            return new Ids.BlockId(text);
        }).toList();
        return new StyleAnalysisWindowFinding(
                StyleCanonical.integer(value, "ordinal", "style_analysis_window"),
                StyleCanonical.string(value, "window_id", "style_analysis_window"),
                blockIds,
                StyleDecisionState.fromCanonicalName(StyleCanonical.string(
                        value, "decision_state", "style_analysis_window"
                )),
                StyleCalibrationConfidence.fromCanonicalName(StyleCanonical.string(
                        value, "confidence", "style_analysis_window"
                )),
                StyleCanonical.bool(
                        value, "can_trigger_rewrite", "style_analysis_window"
                ),
                StyleCanonical.object(
                        value.get("payload"), "style_analysis_window.payload"
                )
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block_ids", blockIds.stream().map(Ids.BlockId::value).toList());
        value.put("can_trigger_rewrite", canTriggerRewrite);
        value.put("confidence", confidence.canonicalName());
        value.put("decision_state", decisionState.canonicalName());
        value.put("ordinal", ordinal);
        value.put("payload", payload);
        value.put("window_id", windowId);
        return CanonicalValues.freezeMap(value, "style_analysis_window");
    }
}
