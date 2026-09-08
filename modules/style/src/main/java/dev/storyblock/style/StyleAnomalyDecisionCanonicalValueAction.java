package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleAnomalyDecisionCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleAnomalyDecision self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("can_trigger_rewrite", self.canTriggerRewrite());
        value.put("confidence", self.confidence().canonicalName());
        value.put("independent_q99_channels", self.independentQ99Channels().stream()
                .map(StyleFeatureChannel::canonicalName).toList());
        value.put("intentional_shift_adjusted", self.intentionalShiftAdjusted());
        value.put("localized_micro_window_ids", self.localizedMicroWindowIds());
        value.put("operational_window_id", self.operationalWindowId());
        value.put("reason", self.reason().canonicalName());
        value.put("state", self.state().canonicalName());
        value.put("sustaining_window_ids", self.sustainingWindowIds());
        return CanonicalValues.freezeMap(value, "style_anomaly_decision");
    }
}
