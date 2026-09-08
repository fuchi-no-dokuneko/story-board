package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleProfileVersionViewCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleProfileVersionView self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("approved_at", self.approvedAt() == null ? null : self.approvedAt().toString());
        value.put("approved_by", self.approvedBy());
        value.put("can_gate_rewrites", self.canGateRewrites());
        value.put("lifecycle", self.lifecycle().stream()
                .map(StyleLifecycleEvent::canonicalValue).toList());
        value.put("profile_version", self.profileVersion().canonicalValue());
        value.put("state", self.state().canonicalName());
        return CanonicalValues.freezeMap(value, "style_profile_version_view");
    }
}
