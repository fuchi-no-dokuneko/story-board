package dev.storyblock.security;

public final class SecretAlreadyIssuedException extends RuntimeException {
    public SecretAlreadyIssuedException() {
        super("The idempotent access key already exists; its one-time secret cannot be replayed");
    }
}
