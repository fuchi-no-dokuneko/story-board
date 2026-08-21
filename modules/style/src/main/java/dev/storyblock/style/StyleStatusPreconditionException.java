package dev.storyblock.style;

public final class StyleStatusPreconditionException extends RuntimeException {
    private final String currentHash;

    public StyleStatusPreconditionException(String currentHash) {
        super("Style resource ETag does not match its current immutable status");
        this.currentHash = currentHash;
    }

    public String currentHash() {
        return currentHash;
    }
}
