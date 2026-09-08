package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.util.List;
import java.util.Map;

final class StyleAnalysisWindowFindingFromCanonicalFactory {
    static StyleAnalysisWindowFinding fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleAnalysisWindowFinding.FIELDS, "style_analysis_window");
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
}
