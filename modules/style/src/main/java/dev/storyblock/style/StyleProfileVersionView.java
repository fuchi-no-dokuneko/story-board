package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StyleProfileVersionView(
        StyleProfileVersion profileVersion,
        List<StyleLifecycleEvent> lifecycle,
        StyleProfileState state,
        String approvedBy,
        Instant approvedAt
) {
    public StyleProfileVersionView {
        Objects.requireNonNull(profileVersion, "profileVersion");
        lifecycle = List.copyOf(lifecycle);
        if (lifecycle.isEmpty()) {
            throw new IllegalArgumentException("Style profile version requires lifecycle events");
        }
        StyleProfileState expected = null;
        int sequence = 0;
        String approvalActor = null;
        Instant approvalTime = null;
        for (StyleLifecycleEvent event : lifecycle) {
            sequence++;
            if (!event.profileId().equals(profileVersion.profileId())
                    || !event.versionId().equals(profileVersion.versionId())
                    || event.sequence() != sequence
                    || event.fromState() != expected) {
                throw new IllegalArgumentException(
                        "Style lifecycle events are not a contiguous version history"
                );
            }
            expected = event.toState();
            if (event.toState() == StyleProfileState.READY) {
                approvalActor = event.auditContext().actorId();
                approvalTime = event.occurredAt();
            }
        }
        if (approvalActor != null
                && profileVersion.content().containsGeneratedText()
                && lifecycle.stream()
                        .filter(event -> event.toState() == StyleProfileState.READY)
                        .noneMatch(StyleLifecycleEvent::generatedPromotionConfirmed)) {
            throw new IllegalArgumentException(
                    "Generated corpus READY approval requires explicit confirmation"
            );
        }
        Objects.requireNonNull(state, "state");
        if (state != expected
                || !Objects.equals(approvedBy, approvalActor)
                || !Objects.equals(approvedAt, approvalTime)) {
            throw new IllegalArgumentException(
                    "Style profile version status does not match lifecycle history"
            );
        }
    }

    public static StyleProfileVersionView of(
            StyleProfileVersion version,
            List<StyleLifecycleEvent> lifecycle
    ) {
        List<StyleLifecycleEvent> events = List.copyOf(lifecycle);
        if (events.isEmpty()) {
            throw new IllegalArgumentException(
                    "Style profile version requires lifecycle events"
            );
        }
        StyleLifecycleEvent current = events.getLast();
        StyleLifecycleEvent approval = events.stream()
                .filter(event -> event.toState() == StyleProfileState.READY)
                .findFirst()
                .orElse(null);
        return new StyleProfileVersionView(
                version,
                events,
                current.toState(),
                approval == null ? null : approval.auditContext().actorId(),
                approval == null ? null : approval.occurredAt()
        );
    }

    public boolean canGateRewrites() {
        return state == StyleProfileState.READY
                && approvedBy != null
                && approvedAt != null
                && profileVersion.content().hasGateCalibration()
                && (!profileVersion.content().containsGeneratedText()
                        || lifecycle.stream().anyMatch(event ->
                                event.toState() == StyleProfileState.READY
                                        && event.generatedPromotionConfirmed()
                        ));
    }

    public String statusHash() {
        return CanonicalJson.hash(canonicalValue());
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("approved_at", approvedAt == null ? null : approvedAt.toString());
        value.put("approved_by", approvedBy);
        value.put("can_gate_rewrites", canGateRewrites());
        value.put("lifecycle", lifecycle.stream()
                .map(StyleLifecycleEvent::canonicalValue).toList());
        value.put("profile_version", profileVersion.canonicalValue());
        value.put("state", state.canonicalName());
        return CanonicalValues.freezeMap(value, "style_profile_version_view");
    }
}
