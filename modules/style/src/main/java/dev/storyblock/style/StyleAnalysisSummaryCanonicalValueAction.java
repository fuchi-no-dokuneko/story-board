package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleAnalysisSummaryCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleAnalysisSummary self)  {
        Map<String, Object> counts = new LinkedHashMap<>();
        for (StyleDecisionState state : StyleDecisionState.values()) {
            counts.put(state.canonicalName(), self.decisionCounts().get(state));
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analyzed_block_count", self.analyzedBlockCount());
        value.put("calibrated_window_count", self.calibratedWindowCount());
        value.put("decision_counts", counts);
        value.put("operational_window_count", self.operationalWindowCount());
        return CanonicalValues.freezeMap(value, "style_analysis_summary");
    }
}
