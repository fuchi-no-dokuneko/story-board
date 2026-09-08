package dev.storyblock.renderer;

import java.util.LinkedHashMap;
import java.util.Map;

final class RenderPacketCanonicalBlock {
    static Map<String, Object> canonicalBlock(RenderedBlock block) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block_id", block.blockId().value());
        value.put("block_version_id", block.blockVersionId().value());
        value.put("local_meta", block.localMetadata().fields());
        value.put("text", block.text());
        if (block.image() != null) {
            value.put("image", block.image().canonicalValue());
        }
        return Map.copyOf(value);
    }
}
