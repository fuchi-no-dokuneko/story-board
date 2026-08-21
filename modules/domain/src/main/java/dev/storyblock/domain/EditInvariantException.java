package dev.storyblock.domain;

public final class EditInvariantException extends IllegalArgumentException {
    public enum Code {
        REVISION_CONFLICT,
        INVALID_BLOCK_ADJACENCY,
        BLOCK_VERSION_CONFLICT,
        DUPLICATE_BLOCK_ID,
        INVALID_OPERATION
    }

    private final Code code;

    public EditInvariantException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() {
        return code;
    }
}
