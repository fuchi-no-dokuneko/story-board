package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleAnalysisWindowFindingCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleAnalysisWindowFinding self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block_ids", self.blockIds().stream().map(Ids.BlockId::value).toList());
        value.put("can_trigger_rewrite", self.canTriggerRewrite());
        value.put("confidence", self.confidence().canonicalName());
        value.put("decision_state", self.decisionState().canonicalName());
        value.put("ordinal", self.ordinal());
        value.put("payload", self.payload());
        value.put("window_id", self.windowId());
        return CanonicalValues.freezeMap(value, "style_analysis_window");
    }
}
