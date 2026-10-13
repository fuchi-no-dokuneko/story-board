package dev.storyblock.domain;

public final class HiddenBlockException extends IllegalArgumentException {
    public enum Code {
        HIDDEN_BLOCK_TEXT_INVALID,
        HIDDEN_BLOCK_META_NOT_ALLOWED,
        HIDDEN_BLOCK_UNSUPPORTED,
        HIDDEN_BLOCK_NOT_RENDERABLE
    }

    private final Code code;

    public HiddenBlockException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() { return code; }
}
