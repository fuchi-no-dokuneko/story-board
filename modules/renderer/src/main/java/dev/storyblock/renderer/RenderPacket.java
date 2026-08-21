package dev.storyblock.renderer;

import dev.storyblock.domain.CanonicalValues;
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
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("novel_id", novelId.value());
        value.put("revision_id", revisionId.value());
        value.put("revision_hash", revisionHash);
        value.put("renderer_version", rendererVersion);
        value.put("range", canonicalRange());
        value.put("rendered_text", renderedText);
        value.put("blocks", blocks.stream().map(RenderPacket::canonicalBlock).toList());
        value.put(
                "resolved_meta",
                resolvedMetadata.stream().map(RenderPacket::canonicalMetadata).toList()
        );
        value.put("offset_map", offsetMap.stream().map(RenderPacket::canonicalOffset).toList());
        value.put(
                "scene_boundaries",
                sceneBoundaries.stream().map(RenderPacket::canonicalBoundary).toList()
        );
        return CanonicalValues.freezeMap(value, "render_packet");
    }

    private Map<String, Object> canonicalRange() {
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

    private static Map<String, Object> canonicalBlock(RenderedBlock block) {
        return Map.of(
                "block_id", block.blockId().value(),
                "block_version_id", block.blockVersionId().value(),
                "local_meta", block.localMetadata().fields(),
                "text", block.text()
        );
    }

    private static Map<String, Object> canonicalMetadata(ResolvedBlockMetadata metadata) {
        return Map.of(
                "after", metadata.after(),
                "before", metadata.before(),
                "block_id", metadata.blockId().value(),
                "events", metadata.events()
        );
    }

    private static Map<String, Object> canonicalOffset(OffsetMapEntry offset) {
        return Map.of(
                "block_id", offset.blockId().value(),
                "rendered_end", offset.renderedEnd(),
                "rendered_start", offset.renderedStart()
        );
    }

    private static Map<String, Object> canonicalBoundary(DerivedSceneBoundary boundary) {
        return Map.of(
                "scene_id", boundary.sceneId().value(),
                "state_in", boundary.stateIn(),
                "state_out", boundary.stateOut()
        );
    }
}
