package dev.storyblock.domain;

import java.util.Map;
import java.util.Objects;

public record BlockDraft(
        Ids.BlockId id,
        String text,
        BlockMetadata metadata,
        Map<String, Object> extensions
) {
    public BlockDraft {
        Objects.requireNonNull(id, "id");
        UnicodeText.validateBlock(text);
        Objects.requireNonNull(metadata, "metadata");
        extensions = CanonicalValues.freezeMap(extensions, "block_draft.extensions");
    }

    public static BlockDraft create(String text, BlockMetadata metadata) {
        return new BlockDraft(Ids.BlockId.create(), text, metadata, Map.of());
    }

    public NarrativeBlock materialize(OrderKey orderKey) {
        return NarrativeBlock.create(id, orderKey, text, metadata, extensions);
    }
}
