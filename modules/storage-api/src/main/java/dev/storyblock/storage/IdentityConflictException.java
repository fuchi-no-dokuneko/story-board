package dev.storyblock.storage;

public final class IdentityConflictException extends RuntimeException {
    private final String identifier;
    public IdentityConflictException(String identifier) {
        super("Identifier already belongs to another entity or immutable version: " + identifier);
        this.identifier = identifier;
    }
    public String identifier() { return identifier; }
}
