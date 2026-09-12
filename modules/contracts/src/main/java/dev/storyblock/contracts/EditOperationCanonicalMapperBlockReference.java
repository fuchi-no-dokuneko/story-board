package dev.storyblock.contracts;

import dev.storyblock.domain.BlockVersionRef;
import java.util.Map;

final class EditOperationCanonicalMapperBlockReference {
    static Map<String, Object> blockReference(BlockVersionRef reference) {
        return Map.of(
                "block_id", reference.blockId().value(),
                "block_version_id", reference.blockVersionId().value()
        );
    }
}
