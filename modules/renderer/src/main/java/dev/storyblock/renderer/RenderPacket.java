package dev.storyblock.renderer;

import dev.storyblock.domain.Ids;
import java.util.List;
import java.util.Objects;

public record RenderPacket(
        Ids.NovelId novelId,
        Ids.RevisionId revisionId,
        String revisionHash,
        String rendererVersion,
        RenderRange range,
        String renderedText,
        List<RenderedBlock> blocks,
        List<ResolvedBlockMetadata> resolvedMetadata,
        List<OffsetMapEntry> offsetMap
) {
    public RenderPacket {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        Objects.requireNonNull(revisionHash, "revisionHash");
        Objects.requireNonNull(rendererVersion, "rendererVersion");
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(renderedText, "renderedText");
        blocks = List.copyOf(blocks);
        resolvedMetadata = List.copyOf(resolvedMetadata);
        offsetMap = List.copyOf(offsetMap);
        if (blocks.size() != resolvedMetadata.size() || blocks.size() != offsetMap.size()) {
            throw new IllegalArgumentException("Render packet lists must identify the same block range");
        }
    }
}
