package dev.storyblock.contracts;

public final class CanonicalPackageException extends IllegalArgumentException {
    public CanonicalPackageException(String message) {
        super(message);
    }

    public CanonicalPackageException(String message, Throwable cause) {
        super(message, cause);
    }
}
