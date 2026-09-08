package dev.storyblock.renderer;

import java.util.Map;

final class RenderPacketCanonicalOffset {
    static Map<String, Object> canonicalOffset(OffsetMapEntry offset) {
        return Map.of(
                "block_id", offset.blockId().value(),
                "rendered_end", offset.renderedEnd(),
                "rendered_start", offset.renderedStart()
        );
    }
}
