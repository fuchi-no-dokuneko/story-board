package dev.storyblock.storage;

import dev.storyblock.domain.Ids;
import java.util.Objects;

public record CanonicalImportResult(
        Ids.NovelId novelId,
        RevisionRef head,
        boolean idempotentReplay
) {
    public CanonicalImportResult {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(head, "head");
    }
}
