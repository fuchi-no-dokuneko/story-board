package dev.storyblock.renderer;

import java.util.Map;

final class RenderPacketCanonicalMetadata {
    static Map<String, Object> canonicalMetadata(ResolvedBlockMetadata metadata) {
        return Map.of(
                "after", metadata.after(),
                "before", metadata.before(),
                "block_id", metadata.blockId().value(),
                "events", metadata.events()
        );
    }
}
