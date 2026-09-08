package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.SceneBoundaryContract;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseBoundary {
    static SceneBoundaryContract parseBoundary(Map<String, Object> value) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(value, Set.of(
                "scene_id", "first_block_id", "last_block_id", "expected_sequence_hash"
        ), "scene_boundary");
        return new SceneBoundaryContract(
                new Ids.SceneId(EditOperationCanonicalMapperString.string(value, "scene_id", "scene_boundary")),
                EditOperationCanonicalMapperOptionalBlockId.optionalBlockId(value.get("first_block_id")),
                EditOperationCanonicalMapperOptionalBlockId.optionalBlockId(value.get("last_block_id")),
                EditOperationCanonicalMapperString.string(value, "expected_sequence_hash", "scene_boundary")
        );
    }
}
