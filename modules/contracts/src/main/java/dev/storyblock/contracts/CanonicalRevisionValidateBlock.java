package dev.storyblock.contracts;

import dev.storyblock.domain.OrderKey;
import dev.storyblock.domain.StableIds;
import dev.storyblock.domain.UnicodeText;
import dev.storyblock.domain.BlockHidden;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.HiddenText;
import dev.storyblock.domain.HiddenBlockException;
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
        CanonicalRevisionValidateExtensions.validateExtensions(block.get("extensions"), path + ".extensions");

        Map<String, Object> meta = CanonicalRevision.requireMap(block.get("meta"), path + ".meta");
        CanonicalRevisionValidateKeys.validateKeys(meta, Set.of(), META_FIELDS, path + ".meta");
        Map<String, Object> extensions = block.get("extensions") == null ? Map.of()
                : CanonicalRevision.requireMap(block.get("extensions"), path + ".extensions");
        BlockHidden.validate(extensions, new BlockMetadata(meta));
        String text = CanonicalRevisionRequireString.requireString(block, "text", path);
        if (BlockHidden.isHidden(extensions)) {
            if (!HiddenText.validate(text).equals(text)) throw new HiddenBlockException(
                    HiddenBlockException.Code.HIDDEN_BLOCK_TEXT_INVALID,
                    "Canonical hidden text must be NFC and stripped");
        }
        else UnicodeText.validateBlock(text);
    }
}
