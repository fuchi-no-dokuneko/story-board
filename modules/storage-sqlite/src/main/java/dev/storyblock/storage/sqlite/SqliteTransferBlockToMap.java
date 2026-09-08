package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.NarrativeBlock;
import java.util.LinkedHashMap;
import java.util.Map;

final class SqliteTransferBlockToMap {
    static Map<String, Object> blockToMap(NarrativeBlock block) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", block.id().value());
        value.put("block_version_id", block.versionId().value());
        value.put("order_key", block.orderKey().value());
        value.put("text", block.text());
        value.put("meta", block.metadata().fields());
        if (!block.extensions().isEmpty()) {
            value.put("extensions", block.extensions());
        }
        return value;
    }
}
