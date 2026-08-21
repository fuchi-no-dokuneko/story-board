package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record StyleLifecycleEvent(
        Ids.StyleLifecycleEventId eventId,
        Ids.StyleProfileId profileId,
        Ids.StyleProfileVersionId versionId,
        int sequence,
        StyleProfileState fromState,
        StyleProfileState toState,
        String reason,
        boolean generatedPromotionConfirmed,
        AuditContext auditContext,
        Instant occurredAt
) {
    private static final Set<String> FIELDS = Set.of(
            "event_id", "profile_id", "version_id", "sequence", "from_state",
            "to_state", "reason", "generated_promotion_confirmed", "request_id",
            "actor_id", "actor_key_id", "occurred_at"
    );

    public StyleLifecycleEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(versionId, "versionId");
        if (sequence < 1) {
            throw new IllegalArgumentException("Style lifecycle sequence must be positive");
        }
        Objects.requireNonNull(toState, "toState");
        if (sequence == 1 && (fromState != null || toState != StyleProfileState.DRAFT)) {
            throw new IllegalArgumentException("First style lifecycle event must create DRAFT");
        }
        if (sequence > 1 && (fromState == null || !fromState.canTransitionTo(toState))) {
            throw new IllegalArgumentException("Style lifecycle transition is invalid");
        }
        if (reason == null || reason.isBlank() || reason.length() > 1_000) {
            throw new IllegalArgumentException("Style lifecycle reason is invalid");
        }
        if (generatedPromotionConfirmed && toState != StyleProfileState.READY) {
            throw new IllegalArgumentException(
                    "Generated corpus confirmation applies only to READY promotion"
            );
        }
        Objects.requireNonNull(auditContext, "auditContext");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (!occurredAt.equals(auditContext.occurredAt())) {
            throw new IllegalArgumentException(
                    "Style lifecycle audit time must match event time"
            );
        }
    }

    public static StyleLifecycleEvent initial(
            Ids.StyleLifecycleEventId eventId,
            Ids.StyleProfileId profileId,
            Ids.StyleProfileVersionId versionId,
            AuditContext auditContext
    ) {
        return new StyleLifecycleEvent(
                eventId,
                profileId,
                versionId,
                1,
                null,
                StyleProfileState.DRAFT,
                "Immutable profile version created",
                false,
                auditContext,
                auditContext.occurredAt()
        );
    }

    public static StyleLifecycleEvent fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_lifecycle_event");
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

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("actor_id", auditContext.actorId());
        value.put("actor_key_id", auditContext.actorKeyId() == null
                ? null : auditContext.actorKeyId().value());
        value.put("event_id", eventId.value());
        value.put("from_state", fromState == null ? null : fromState.canonicalName());
        value.put("generated_promotion_confirmed", generatedPromotionConfirmed);
        value.put("occurred_at", occurredAt.toString());
        value.put("profile_id", profileId.value());
        value.put("reason", reason);
        value.put("request_id", auditContext.requestId());
        value.put("sequence", sequence);
        value.put("to_state", toState.canonicalName());
        value.put("version_id", versionId.value());
        return CanonicalValues.freezeMap(value, "style_lifecycle_event");
    }
}
