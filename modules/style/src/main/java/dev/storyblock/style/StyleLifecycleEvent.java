package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.time.Instant;
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
  static final Set<String> FIELDS = Set.of(
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
    return StyleLifecycleEventFromCanonicalFactory.fromCanonical(value);
  }

  public Map<String, Object> canonicalValue() {
    return StyleLifecycleEventCanonicalValueAction.canonicalValue(this);
  }
}
