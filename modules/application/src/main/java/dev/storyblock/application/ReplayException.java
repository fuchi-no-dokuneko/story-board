package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import java.util.Objects;

public final class ReplayException extends RuntimeException {
    private final Ids.NovelId novelId;
    private final long sequence;

    public ReplayException(Ids.NovelId novelId, long sequence, String message) {
        super(message);
        this.novelId = Objects.requireNonNull(novelId, "novelId");
        this.sequence = sequence;
    }

    public ReplayException(
            Ids.NovelId novelId,
            long sequence,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.novelId = Objects.requireNonNull(novelId, "novelId");
        this.sequence = sequence;
    }

    public Ids.NovelId novelId() {
        return novelId;
    }

    public long sequence() {
        return sequence;
    }
}
