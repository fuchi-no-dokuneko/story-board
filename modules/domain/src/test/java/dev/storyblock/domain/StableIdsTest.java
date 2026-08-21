package dev.storyblock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StableIdsTest {
    @Test
    void generatesTypedUuidVersionSevenIdentifiers() {
        Ids.BlockId first = Ids.BlockId.create();
        Ids.BlockId second = Ids.BlockId.create();

        assertNotEquals(first, second);
        UUID uuid = UUID.fromString(first.value().substring("blk_".length()));
        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());
    }

    @Test
    void rejectsWrongIdentityType() {
        String novel = Ids.NovelId.create().value();
        assertThrows(IllegalArgumentException.class, () -> new Ids.BlockId(novel));
    }

    @Test
    void derivesRepeatableDistinctUuidVersionSevenIdentifiers() {
        Ids.OperationId operationId = Ids.OperationId.create();

        String first = StableIds.derive("blv", operationId.value(), "first");
        String repeated = StableIds.derive("blv", operationId.value(), "first");
        String second = StableIds.derive("blv", operationId.value(), "second");

        assertEquals(first, repeated);
        assertNotEquals(first, second);
        new Ids.BlockVersionId(first);
        new Ids.BlockVersionId(second);
    }
}
