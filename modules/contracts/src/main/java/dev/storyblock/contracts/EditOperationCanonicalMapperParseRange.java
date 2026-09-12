package dev.storyblock.contracts;

import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.BlockVersionRef;
import dev.storyblock.domain.Ids;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseRange {
    static BlockRangeGuard parseRange(Map<String, Object> value) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(value, Set.of(
                "scene_id", "first_block_id", "last_block_id", "expected_blocks",
                "expected_range_hash", "expected_previous_block_id", "expected_next_block_id"
        ), "block_range");
        List<BlockVersionRef> blocks = EditOperationCanonicalMapperArray.array(value.get("expected_blocks"), "expected_blocks")
                .stream()
                .map(entry -> EditOperationCanonicalMapperParseBlockReference.parseBlockReference(EditOperationCanonicalMapper.object(entry, "block_reference")))
                .toList();
        if (!blocks.getFirst().blockId().value().equals(EditOperationCanonicalMapperString.string(value, "first_block_id", "block_range"))
                || !blocks.getLast().blockId().value().equals(
                        EditOperationCanonicalMapperString.string(value, "last_block_id", "block_range")
                )) {
            throw new IllegalArgumentException("Block range endpoints do not match expected_blocks");
        }
        return new BlockRangeGuard(
                new Ids.SceneId(EditOperationCanonicalMapperString.string(value, "scene_id", "block_range")),
                blocks,
                EditOperationCanonicalMapperOptionalBlockId.optionalBlockId(value.get("expected_previous_block_id")),
                EditOperationCanonicalMapperOptionalBlockId.optionalBlockId(value.get("expected_next_block_id")),
                EditOperationCanonicalMapperString.string(value, "expected_range_hash", "block_range")
        );
    }
}
