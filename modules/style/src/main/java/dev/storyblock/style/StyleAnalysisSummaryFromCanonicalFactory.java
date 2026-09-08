package dev.storyblock.style;

import java.util.EnumMap;
import java.util.Map;

final class StyleAnalysisSummaryFromCanonicalFactory {
    static StyleAnalysisSummary fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleAnalysisSummary.FIELDS, "style_analysis_summary");
        Map<String, Object> counts = StyleCanonical.object(
                value.get("decision_counts"), "style_analysis_summary.decision_counts"
        );
        EnumMap<StyleDecisionState, Integer> parsed = new EnumMap<>(
                StyleDecisionState.class
        );
        counts.forEach((name, raw) -> {
            if (!(raw instanceof Number)) {
                throw new IllegalArgumentException(
                        "Style analysis decision count must be an integer"
                );
            }
            parsed.put(
                    StyleDecisionState.fromCanonicalName(name),
                    StyleCanonical.integer(
                            Map.of("count", raw), "count", "style_analysis_summary"
                    )
            );
        });
        return new StyleAnalysisSummary(
                StyleCanonical.integer(
                        value, "analyzed_block_count", "style_analysis_summary"
                ),
                StyleCanonical.integer(
                        value, "operational_window_count", "style_analysis_summary"
                ),
                StyleCanonical.integer(
                        value, "calibrated_window_count", "style_analysis_summary"
                ),
                parsed
        );
    }
}
