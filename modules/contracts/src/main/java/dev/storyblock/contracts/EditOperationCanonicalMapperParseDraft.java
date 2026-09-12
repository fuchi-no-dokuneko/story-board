package dev.storyblock.contracts;

import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class EditOperationCanonicalMapperParseDraft {
    static BlockDraft parseDraft(Map<String, Object> value) {
        EditOperationCanonicalMapperRequireKeys.requireKeys(
                value,
                Set.of("id", "text", "meta"),
                Set.of("extensions", "image"),
                "block_draft"
        );
        Map<String, Object> extensions = value.containsKey("extensions")
                ? new LinkedHashMap<>(EditOperationCanonicalMapper.object(
                        value.get("extensions"), "block_draft.extensions"
                ))
                : new LinkedHashMap<>();
        if (extensions.containsKey(BlockImage.EXTENSION_KEY)) {
            throw new IllegalArgumentException(
                    "block_draft must declare storyblock.image through the image field"
            );
        }
        if (value.containsKey("image")) {
            extensions.put(
                    BlockImage.EXTENSION_KEY,
                    EditOperationCanonicalMapper.object(value.get("image"), "block_draft.image")
            );
        }
        return new BlockDraft(
                new Ids.BlockId(EditOperationCanonicalMapperString.string(value, "id", "block_draft")),
                EditOperationCanonicalMapperString.string(value, "text", "block_draft"),
                new BlockMetadata(EditOperationCanonicalMapper.object(value.get("meta"), "block_draft.meta")),
                extensions
        );
    }
}
