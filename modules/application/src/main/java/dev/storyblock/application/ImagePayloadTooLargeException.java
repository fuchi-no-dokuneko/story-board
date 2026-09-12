package dev.storyblock.application;

public final class ImagePayloadTooLargeException extends RuntimeException {
    private final int limitBytes;

    public ImagePayloadTooLargeException(int limitBytes) {
        super("Image exceeds the configured byte limit of " + limitBytes);
        this.limitBytes = limitBytes;
    }

    public int limitBytes() {
        return limitBytes;
    }
}
