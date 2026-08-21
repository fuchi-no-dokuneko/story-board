package dev.storyblock.application;

import java.util.Objects;

public final class CommitRejectedException extends RuntimeException {
    public static final int HTTP_STATUS = 422;

    private final PreviewResponse preview;

    public CommitRejectedException(PreviewResponse preview) {
        super("Commit candidate failed deterministic validation");
        this.preview = Objects.requireNonNull(preview, "preview");
        if (preview.committable()) {
            throw new IllegalArgumentException("A committable preview cannot be rejected");
        }
    }

    public PreviewResponse preview() {
        return preview;
    }
}
