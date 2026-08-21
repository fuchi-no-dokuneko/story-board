package dev.storyblock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class VersionedBlockTest {
    private static final String META_A = "sha256:" + "a".repeat(64);
    private static final String META_B = "sha256:" + "b".repeat(64);

    @Test
    void movingPreservesStableBlockAndVersionIdentity() {
        VersionedBlock original = VersionedBlock.create(OrderKey.initial(), "第一句。", META_A);
        VersionedBlock moved = original.moveTo(OrderKey.between(original.orderKey(), null));

        assertEquals(original.blockId(), moved.blockId());
        assertEquals(original.blockVersionId(), moved.blockVersionId());
        assertNotEquals(original.orderKey(), moved.orderKey());
    }

    @Test
    void textOrCanonicalMetadataChangeCreatesANewVersionIdentity() {
        VersionedBlock original = VersionedBlock.create(OrderKey.initial(), "第一句。", META_A);
        VersionedBlock rewritten = original.revise("改寫後。", META_A);
        VersionedBlock metadataCorrected = rewritten.revise(rewritten.text(), META_B);

        assertEquals(original.blockId(), rewritten.blockId());
        assertNotEquals(original.blockVersionId(), rewritten.blockVersionId());
        assertEquals(rewritten.blockId(), metadataCorrected.blockId());
        assertNotEquals(rewritten.blockVersionId(), metadataCorrected.blockVersionId());
        assertSame(metadataCorrected, metadataCorrected.revise(metadataCorrected.text(), META_B));
    }
}
