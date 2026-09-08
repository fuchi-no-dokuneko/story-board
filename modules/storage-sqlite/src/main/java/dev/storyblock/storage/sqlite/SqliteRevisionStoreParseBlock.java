package dev.storyblock.storage.sqlite;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.util.Map;

final class SqliteRevisionStoreParseBlock {
    static NarrativeBlock parseBlock(String json) {
        @SuppressWarnings("unchecked")
        Map<String, Object> value = CanonicalJson.mapper().readValue(json, Map.class);
        return new NarrativeBlock(
                new Ids.BlockId(SqliteRevisionStoreRequiredString.requiredString(value, "id")),
                new Ids.BlockVersionId(SqliteRevisionStoreRequiredString.requiredString(value, "block_version_id")),
                new OrderKey(SqliteRevisionStoreRequiredString.requiredString(value, "order_key")),
                SqliteRevisionStoreRequiredString.requiredString(value, "text"),
                new BlockMetadata(SqliteRevisionStore.requiredMap(value, "meta")),
                value.containsKey("extensions") ? SqliteRevisionStore.requiredMap(value, "extensions") : Map.of()
        );
    }
}
