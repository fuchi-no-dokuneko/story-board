package dev.storyblock.rewrite.policy;

public final class RewriteReservationConflictException extends RuntimeException {
    public RewriteReservationConflictException() {
        super("Rewrite reservation idempotency key has another request");
    }
}
