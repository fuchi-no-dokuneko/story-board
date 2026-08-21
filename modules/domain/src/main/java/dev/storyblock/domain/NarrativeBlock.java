package dev.storyblock.domain;

import java.util.Map;
import java.util.Objects;

public record NarrativeBlock(
        Ids.BlockId id,
        Ids.BlockVersionId versionId,
        OrderKey orderKey,
        String text,
        BlockMetadata metadata,
        Map<String, Object> extensions
) {
    public NarrativeBlock {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(versionId, "versionId");
        Objects.requireNonNull(orderKey, "orderKey");
        UnicodeText.validateBlock(text);
        Objects.requireNonNull(metadata, "metadata");
        extensions = CanonicalValues.freezeMap(extensions, "block.extensions");
    }

    public static NarrativeBlock create(
            Ids.BlockId id,
            OrderKey orderKey,
            String text,
            BlockMetadata metadata,
            Map<String, Object> extensions
    ) {
        return new NarrativeBlock(
                id,
                Ids.BlockVersionId.create(),
                orderKey,
                text,
                metadata,
                extensions
        );
    }

    public NarrativeBlock moveTo(OrderKey newOrderKey) {
        return new NarrativeBlock(id, versionId, newOrderKey, text, metadata, extensions);
    }

    public NarrativeBlock revise(String newText, BlockMetadata newMetadata, Map<String, Object> newExtensions) {
        if (text.equals(newText) && metadata.equals(newMetadata) && extensions.equals(newExtensions)) {
            return this;
        }
        return new NarrativeBlock(
                id,
                Ids.BlockVersionId.create(),
                orderKey,
                newText,
                newMetadata,
                newExtensions
        );
    }
}
