package dev.storyblock.domain;

import java.util.Map;
import java.util.Objects;

public record BlockDraft(
        Ids.BlockId id,
        String text,
        BlockMetadata metadata,
        Map<String, Object> extensions
) implements NarrativeText {
    public BlockDraft {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(metadata, "metadata");
        extensions = CanonicalValues.freezeMap(extensions, "block_draft.extensions");
        BlockHidden.validate(extensions, metadata);
        if (BlockHidden.isHidden(extensions)) text = HiddenText.validate(text);
        BlockImage.fromExtensions(extensions);
    }

    public static BlockDraft create(String text, BlockMetadata metadata) {
        return new BlockDraft(Ids.BlockId.create(), text, metadata, Map.of());
    }

    public static BlockDraft createImage(
            String caption,
            BlockMetadata metadata,
            BlockImage image
    ) {
        Objects.requireNonNull(image, "image");
        return new BlockDraft(
                Ids.BlockId.create(), caption, metadata, image.attachTo(Map.of())
        );
    }

    public java.util.Optional<BlockImage> image() {
        return BlockImage.fromExtensions(extensions);
    }

    public boolean isHidden() { return BlockHidden.isHidden(extensions); }

    public NarrativeBlock materialize(OrderKey orderKey) {
        return materialize(orderKey, Ids.BlockVersionId.create());
    }

    public NarrativeBlock materialize(OrderKey orderKey, Ids.BlockVersionId versionId) {
        return new NarrativeBlock(id, versionId, orderKey, text, metadata, extensions);
    }
}
