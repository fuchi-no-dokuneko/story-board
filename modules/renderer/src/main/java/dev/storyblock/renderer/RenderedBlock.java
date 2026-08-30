package dev.storyblock.renderer;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.Ids;
import java.util.Objects;

public record RenderedBlock(
        Ids.BlockId blockId,
        Ids.BlockVersionId blockVersionId,
        String text,
        BlockMetadata localMetadata,
        BlockImage image
) {
    public RenderedBlock {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(blockVersionId, "blockVersionId");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(localMetadata, "localMetadata");
    }

    public RenderedBlock(
            Ids.BlockId blockId,
            Ids.BlockVersionId blockVersionId,
            String text,
            BlockMetadata localMetadata
    ) {
        this(blockId, blockVersionId, text, localMetadata, null);
    }
}
