package dev.storyblock.security;

public final class AccessKeyRequestConflictException extends RuntimeException {
    public AccessKeyRequestConflictException() {
        super("The access-key idempotency key was reused for different parameters");
    }
}
