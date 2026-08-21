package dev.storyblock.renderer;

import dev.storyblock.domain.Ids;
import java.util.Objects;

public record OffsetMapEntry(
        Ids.BlockId blockId,
        int renderedStart,
        int renderedEnd
) {
    public OffsetMapEntry {
        Objects.requireNonNull(blockId, "blockId");
        if (renderedStart < 0 || renderedEnd < renderedStart) {
            throw new IllegalArgumentException("Invalid rendered offset range");
        }
    }
}
