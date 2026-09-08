package dev.storyblock.contracts;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.util.Map;

final class NarrativeCanonicalMapperBlockFromCanonical {
    static NarrativeBlock blockFromCanonical(Map<String, Object> block) {
        return new NarrativeBlock(
                new Ids.BlockId(NarrativeCanonicalMapperRequireString.requireString(block, "id")),
                new Ids.BlockVersionId(NarrativeCanonicalMapperRequireString.requireString(block, "block_version_id")),
                new OrderKey(NarrativeCanonicalMapperRequireString.requireString(block, "order_key")),
                NarrativeCanonicalMapperRequireString.requireString(block, "text"),
                new BlockMetadata(NarrativeCanonicalMapper.requireMap(block.get("meta"), "block.meta")),
                NarrativeCanonicalMapperOptionalMap.optionalMap(block.get("extensions"), "block.extensions")
        );
    }
}
