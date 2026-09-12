package dev.storyblock.contracts;

import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.StableIds;
import dev.storyblock.domain.UnicodeText;
import java.util.Map;
import java.util.Set;
import static dev.storyblock.contracts.CanonicalRevisionFields.BLOCK_REQUIRED;
import static dev.storyblock.contracts.CanonicalRevisionFields.BLOCK_OPTIONAL;
import static dev.storyblock.contracts.CanonicalRevisionFields.META_FIELDS;

final class CanonicalRevisionValidateBlock {
    static void validateBlock(
            Map<String, Object> block,
            String scenePath,
            int blockIndex
    ) {
        String path = scenePath + ".block[" + blockIndex + "]";
        CanonicalRevisionValidateKeys.validateKeys(block, BLOCK_REQUIRED, BLOCK_OPTIONAL, path);
        StableIds.require(CanonicalRevisionRequireString.requireString(block, "id", path), "blk");
        StableIds.require(CanonicalRevisionRequireString.requireString(block, "block_version_id", path), "blv");
        new OrderKey(CanonicalRevisionRequireString.requireString(block, "order_key", path));
        UnicodeText.validateBlock(CanonicalRevisionRequireString.requireString(block, "text", path));
        CanonicalRevisionValidateExtensions.validateExtensions(block.get("extensions"), path + ".extensions");

        Map<String, Object> meta = CanonicalRevision.requireMap(block.get("meta"), path + ".meta");
        CanonicalRevisionValidateKeys.validateKeys(meta, Set.of(), META_FIELDS, path + ".meta");
    }
}
