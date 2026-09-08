package dev.storyblock.contracts;

import dev.storyblock.domain.InsertionPoint;
import java.util.LinkedHashMap;
import java.util.Map;

final class EditOperationCanonicalMapperInsertionPoint {
    static Map<String, Object> insertionPoint(InsertionPoint point) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene_id", point.sceneId().value());
        result.put("position", point.position().name().toLowerCase(java.util.Locale.ROOT));
        if (point.anchorBlockId() != null) {
            result.put("anchor_block_id", point.anchorBlockId().value());
        }
        return Map.copyOf(result);
    }
}
