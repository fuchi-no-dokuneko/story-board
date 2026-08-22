package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record StyleAnalysisSummary(
        int analyzedBlockCount,
        int operationalWindowCount,
        int calibratedWindowCount,
        Map<StyleDecisionState, Integer> decisionCounts
) {
    private static final Set<String> FIELDS = Set.of(
            "analyzed_block_count", "operational_window_count",
            "calibrated_window_count", "decision_counts"
    );

    public StyleAnalysisSummary {
        if (analyzedBlockCount < 1 || analyzedBlockCount > StyleAnalysisSnapshot.MAX_BLOCKS
                || operationalWindowCount < 0
                || calibratedWindowCount < 0
                || calibratedWindowCount > operationalWindowCount) {
            throw new IllegalArgumentException("Style analysis summary counts are invalid");
        }
        EnumMap<StyleDecisionState, Integer> normalized = new EnumMap<>(
                StyleDecisionState.class
        );
        for (StyleDecisionState state : StyleDecisionState.values()) {
            int count = decisionCounts.getOrDefault(state, 0);
            if (count < 0) {
                throw new IllegalArgumentException(
                        "Style analysis decision count cannot be negative"
                );
            }
            normalized.put(state, count);
        }
        if (decisionCounts.keySet().stream().anyMatch(java.util.Objects::isNull)
                || normalized.values().stream().mapToInt(Integer::intValue).sum()
                != operationalWindowCount
                || operationalWindowCount
                - normalized.get(StyleDecisionState.LOW_CONFIDENCE)
                != calibratedWindowCount) {
            throw new IllegalArgumentException(
                    "Style analysis decision counts do not match window counts"
            );
        }
        decisionCounts = Map.copyOf(normalized);
    }

    public static StyleAnalysisSummary fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_analysis_summary");
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

    public Map<String, Object> canonicalValue() {
        Map<String, Object> counts = new LinkedHashMap<>();
        for (StyleDecisionState state : StyleDecisionState.values()) {
            counts.put(state.canonicalName(), decisionCounts.get(state));
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analyzed_block_count", analyzedBlockCount);
        value.put("calibrated_window_count", calibratedWindowCount);
        value.put("decision_counts", counts);
        value.put("operational_window_count", operationalWindowCount);
        return CanonicalValues.freezeMap(value, "style_analysis_summary");
    }
}
