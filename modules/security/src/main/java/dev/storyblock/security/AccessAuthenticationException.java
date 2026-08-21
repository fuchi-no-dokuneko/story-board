package dev.storyblock.security;

public final class AccessAuthenticationException extends RuntimeException {
    public AccessAuthenticationException() {
        super("Bearer credential is invalid");
    }
}
