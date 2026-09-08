package dev.storyblock.renderer;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class RenderPacketCanonicalValueAction {
    static Map<String, Object> canonicalValue(RenderPacket self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("novel_id", self.novelId().value());
        value.put("revision_id", self.revisionId().value());
        value.put("revision_hash", self.revisionHash());
        value.put("renderer_version", self.rendererVersion());
        value.put("range", self.canonicalRange());
        value.put("rendered_text", self.renderedText());
        value.put("blocks", self.blocks().stream().map(RenderPacketCanonicalBlock::canonicalBlock).toList());
        value.put(
                "resolved_meta",
                self.resolvedMetadata().stream().map(RenderPacketCanonicalMetadata::canonicalMetadata).toList()
        );
        value.put("offset_map", self.offsetMap().stream().map(RenderPacketCanonicalOffset::canonicalOffset).toList());
        value.put(
                "scene_boundaries",
                self.sceneBoundaries().stream().map(RenderPacketCanonicalBoundary::canonicalBoundary).toList()
        );
        return CanonicalValues.freezeMap(value, "render_packet");
    }
}
