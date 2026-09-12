package dev.storyblock.contracts;

import dev.storyblock.domain.SceneBoundaryContract;
import java.util.LinkedHashMap;
import java.util.Map;

final class EditOperationCanonicalMapperBoundary {
    static Map<String, Object> boundary(SceneBoundaryContract boundary) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene_id", boundary.sceneId().value());
        result.put("first_block_id", boundary.firstBlockId() == null
                ? null : boundary.firstBlockId().value());
        result.put("last_block_id", boundary.lastBlockId() == null
                ? null : boundary.lastBlockId().value());
        result.put("expected_sequence_hash", boundary.expectedSequenceHash());
        return java.util.Collections.unmodifiableMap(result);
    }
}
