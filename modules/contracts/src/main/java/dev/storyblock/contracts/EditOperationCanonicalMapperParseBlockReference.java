package dev.storyblock.contracts;

import dev.storyblock.domain.BlockVersionRef;
import dev.storyblock.domain.Ids;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseBlockReference {
    static BlockVersionRef parseBlockReference(Map<String, Object> value) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(value, Set.of("block_id", "block_version_id"), "block_reference");
        return new BlockVersionRef(
                new Ids.BlockId(EditOperationCanonicalMapperString.string(value, "block_id", "block_reference")),
                new Ids.BlockVersionId(EditOperationCanonicalMapperString.string(value, "block_version_id", "block_reference"))
        );
    }
}
