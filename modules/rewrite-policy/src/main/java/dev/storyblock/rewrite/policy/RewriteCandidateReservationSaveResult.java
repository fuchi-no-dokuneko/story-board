package dev.storyblock.rewrite.policy;

import java.util.Objects;

public record RewriteCandidateReservationSaveResult(
        RewriteCandidateReservation reservation,
        boolean idempotentReplay
) {
    public RewriteCandidateReservationSaveResult {
        Objects.requireNonNull(reservation, "reservation");
    }
}
