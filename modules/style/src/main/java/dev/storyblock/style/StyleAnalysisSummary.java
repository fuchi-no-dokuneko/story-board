package dev.storyblock.style;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public record StyleAnalysisSummary(
        int analyzedBlockCount,
        int operationalWindowCount,
        int calibratedWindowCount,
        Map<StyleDecisionState, Integer> decisionCounts
) {
    static final Set<String> FIELDS = Set.of(
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
        return StyleAnalysisSummaryFromCanonicalFactory.fromCanonical(value);
    }

    public Map<String, Object> canonicalValue() {
        return StyleAnalysisSummaryCanonicalValueAction.canonicalValue(this);
    }
}
