package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.time.Instant;
import java.util.Map;

final class StyleLifecycleEventFromCanonicalFactory {
    static StyleLifecycleEvent fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleLifecycleEvent.FIELDS, "style_lifecycle_event");
        String from = StyleCanonical.optionalString(
                value, "from_state", "style_lifecycle_event"
        );
        String actorKey = StyleCanonical.optionalString(
                value, "actor_key_id", "style_lifecycle_event"
        );
        Instant occurredAt = StyleCanonical.instant(
                value, "occurred_at", "style_lifecycle_event"
        );
        return new StyleLifecycleEvent(
                new Ids.StyleLifecycleEventId(StyleCanonical.string(
                        value, "event_id", "style_lifecycle_event"
                )),
                new Ids.StyleProfileId(StyleCanonical.string(
                        value, "profile_id", "style_lifecycle_event"
                )),
                new Ids.StyleProfileVersionId(StyleCanonical.string(
                        value, "version_id", "style_lifecycle_event"
                )),
                StyleCanonical.integer(value, "sequence", "style_lifecycle_event"),
                from == null ? null : StyleProfileState.fromCanonicalName(from),
                StyleProfileState.fromCanonicalName(StyleCanonical.string(
                        value, "to_state", "style_lifecycle_event"
                )),
                StyleCanonical.string(value, "reason", "style_lifecycle_event"),
                StyleCanonical.bool(
                        value, "generated_promotion_confirmed", "style_lifecycle_event"
                ),
                new AuditContext(
                        StyleCanonical.string(value, "request_id", "style_lifecycle_event"),
                        StyleCanonical.string(value, "actor_id", "style_lifecycle_event"),
                        actorKey == null ? null : new Ids.AccessKeyId(actorKey),
                        occurredAt
                ),
                occurredAt
        );
    }
}
