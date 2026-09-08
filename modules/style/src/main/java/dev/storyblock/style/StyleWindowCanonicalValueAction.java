package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.Map;

final class StyleWindowCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleWindow self)  {
        Map<String, Object> value = StyleWindowContentValue.contentValue(
                self.kind(),
                self.segment(),
                self.requestedStratum(),
                self.pov(),
                self.narrativeMode(),
                self.blockIds(),
                self.graphemeCount(),
                self.fullSized(),
                self.intentionalStyleShiftReason()
        );
        value.put("window_id", self.windowId());
        value.put("localization_only", self.localizationOnly());
        value.put("primary_decision_eligible", self.primaryDecisionEligible());
        value.put("sustainment_eligible", self.sustainmentEligible());
        return CanonicalValues.freezeMap(value, "style_window");
    }
}
