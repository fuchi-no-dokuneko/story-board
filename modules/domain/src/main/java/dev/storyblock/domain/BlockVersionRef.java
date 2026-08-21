package dev.storyblock.domain;

import java.util.Objects;

public record BlockVersionRef(Ids.BlockId blockId, Ids.BlockVersionId blockVersionId) {
    public BlockVersionRef {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(blockVersionId, "blockVersionId");
    }

    public static BlockVersionRef from(NarrativeBlock block) {
        return new BlockVersionRef(block.id(), block.versionId());
    }
}
