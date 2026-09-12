package dev.storyblock.contracts;

import dev.storyblock.domain.BlockVersionRef;
import dev.storyblock.domain.Ids;
import java.util.List;
import java.util.Map;

final class EditOperationCanonicalMapperProvenance {
    static List<Map<String, Object>> provenance(
            Map<BlockVersionRef, List<dev.storyblock.domain.Ids.BlockId>> mapping
    ) {
        return mapping.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "source", EditOperationCanonicalMapperBlockReference.blockReference(entry.getKey()),
                        "result_block_ids", entry.getValue().stream()
                                .map(dev.storyblock.domain.Ids.BlockId::value)
                                .toList()
                ))
                .toList();
    }
}
