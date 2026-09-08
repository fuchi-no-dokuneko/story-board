package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleLifecycleEventCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleLifecycleEvent self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("actor_id", self.auditContext().actorId());
        value.put("actor_key_id", self.auditContext().actorKeyId() == null
                ? null : self.auditContext().actorKeyId().value());
        value.put("event_id", self.eventId().value());
        value.put("from_state", self.fromState() == null ? null : self.fromState().canonicalName());
        value.put("generated_promotion_confirmed", self.generatedPromotionConfirmed());
        value.put("occurred_at", self.occurredAt().toString());
        value.put("profile_id", self.profileId().value());
        value.put("reason", self.reason());
        value.put("request_id", self.auditContext().requestId());
        value.put("sequence", self.sequence());
        value.put("to_state", self.toState().canonicalName());
        value.put("version_id", self.versionId().value());
        return CanonicalValues.freezeMap(value, "style_lifecycle_event");
    }
}
