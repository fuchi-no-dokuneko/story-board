package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.InsertionPoint;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseInsertionPoint {
    static InsertionPoint parseInsertionPoint(Map<String, Object> value) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(
                value,
                Set.of("scene_id", "position"),
                Set.of("anchor_block_id"),
                "insertion_point"
        );
        return new InsertionPoint(
                new Ids.SceneId(EditOperationCanonicalMapperString.string(value, "scene_id", "insertion_point")),
                value.containsKey("anchor_block_id")
                        ? new Ids.BlockId(EditOperationCanonicalMapperString.string(value, "anchor_block_id", "insertion_point"))
                        : null,
                InsertionPoint.Position.valueOf(
                        EditOperationCanonicalMapperString.string(value, "position", "insertion_point")
                                .toUpperCase(java.util.Locale.ROOT)
                )
        );
    }
}
