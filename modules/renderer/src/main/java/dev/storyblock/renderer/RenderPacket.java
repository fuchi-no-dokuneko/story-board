package dev.storyblock.renderer;

import dev.storyblock.domain.DerivedSceneBoundary;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        List<OffsetMapEntry> offsetMap,
        List<DerivedSceneBoundary> sceneBoundaries
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
        sceneBoundaries = List.copyOf(sceneBoundaries);
        if (blocks.size() != resolvedMetadata.size() || blocks.size() != offsetMap.size()) {
            throw new IllegalArgumentException("Render packet lists must identify the same block range");
        }
        for (int index = 0; index < blocks.size(); index++) {
            Ids.BlockId blockId = blocks.get(index).blockId();
            if (!blockId.equals(resolvedMetadata.get(index).blockId())
                    || !blockId.equals(offsetMap.get(index).blockId())) {
                throw new IllegalArgumentException(
                        "Render packet block, metadata, and offset identities must align"
                );
            }
        }
    }

    public Map<String, Object> canonicalValue() {
        return RenderPacketCanonicalValueAction.canonicalValue(this);
    }

    Map<String, Object> canonicalRange() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put(
                "from_block_id",
                range.fromBlockId() == null ? null : range.fromBlockId().value()
        );
        value.put(
                "to_block_id",
                range.toBlockId() == null ? null : range.toBlockId().value()
        );
        return value;
    }

}
