package dev.storyblock.style;

import java.util.List;

final class StyleProfileVersionViewOfFactory {
    static StyleProfileVersionView of(StyleProfileVersion version, List<StyleLifecycleEvent> lifecycle)  {
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
}
