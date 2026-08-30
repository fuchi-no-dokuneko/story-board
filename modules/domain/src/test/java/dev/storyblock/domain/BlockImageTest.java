package dev.storyblock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class BlockImageTest {
    private static final String HASH = "sha256:" + "a".repeat(64);

    @Test
    void imageDescriptorRoundTripsThroughNamespacedBlockExtension() {
        BlockImage image = new BlockImage(
                Ids.ArtifactId.create(), HASH, "image/png", 1024, 768, "岑霧立於白色背景。"
        );
        BlockDraft draft = BlockDraft.createImage(
                "岑霧握着黃銅鑰匙。", BlockMetadata.empty(), image
        );

        assertEquals(image, draft.image().orElseThrow());
        assertTrue(draft.extensions().containsKey(BlockImage.EXTENSION_KEY));
        assertEquals(image, draft.materialize(OrderKey.initial()).image().orElseThrow());
    }

    @Test
    void malformedOrUnsafeDescriptorsAreRejectedAtBlockBoundary() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BlockImage(
                        Ids.ArtifactId.create(), HASH, "image/svg+xml", 100, 100, "圖像"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new NarrativeBlock(
                        Ids.BlockId.create(),
                        Ids.BlockVersionId.create(),
                        OrderKey.initial(),
                        "圖像說明。",
                        BlockMetadata.empty(),
                        Map.of(BlockImage.EXTENSION_KEY, Map.of("artifact_id", "bad"))
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new BlockImage(
                        Ids.ArtifactId.create(), HASH, "image/png", 8192, 8192, "圖像"
                )
        );
    }
}
