package dev.storyblock.security;

public final class CrossNovelAccessException extends RuntimeException {
    public CrossNovelAccessException() {
        super("Resource is outside the credential novel boundary");
    }
}
