package dev.storyblock.contracts;

import dev.storyblock.domain.NarrativeBlock;
import java.util.LinkedHashMap;
import java.util.Map;

final class NarrativeCanonicalMapperBlockToCanonical {
    static Map<String, Object> blockToCanonical(NarrativeBlock block) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", block.id().value());
        result.put("block_version_id", block.versionId().value());
        result.put("order_key", block.orderKey().value());
        result.put("text", block.text());
        result.put("meta", block.metadata().fields());
        NarrativeCanonicalMapperPutExtensions.putExtensions(result, block.extensions());
        return result;
    }
}
