package dev.storyblock.contracts;

import dev.storyblock.domain.BlockRangeGuard;
import java.util.LinkedHashMap;
import java.util.Map;

final class EditOperationCanonicalMapperRange {
    static Map<String, Object> range(BlockRangeGuard range) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene_id", range.sceneId().value());
        result.put("first_block_id", range.firstBlockId().value());
        result.put("last_block_id", range.lastBlockId().value());
        result.put("expected_blocks", range.expectedBlocks().stream()
                .map(EditOperationCanonicalMapperBlockReference::blockReference)
                .toList());
        result.put("expected_range_hash", range.expectedRangeHash());
        result.put(
                "expected_previous_block_id",
                range.expectedPreviousBlockId() == null ? null : range.expectedPreviousBlockId().value()
        );
        result.put(
                "expected_next_block_id",
                range.expectedNextBlockId() == null ? null : range.expectedNextBlockId().value()
        );
        return java.util.Collections.unmodifiableMap(result);
    }
}
