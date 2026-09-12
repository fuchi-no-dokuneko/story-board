package dev.storyblock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class StableIdsTest {
    @Test
    void generatesUniqueShortIdentifiersForAllSixTypes() {
        for (String prefix : ShortIds.PREFIXES) {
            var seen = new HashSet<String>();
            for (int i = 0; i < 2000; i++) {
                String id = StableIds.generate(prefix);
                assertTrue(id.matches(prefix + "_[A-Za-z0-9]{5}"));
                assertTrue(seen.add(id));
            }
        }
    }

    @Test
    void rejectsWrongIdentityType() {
        String novel = Ids.NovelId.create().value();
        assertThrows(IllegalArgumentException.class, () -> new Ids.BlockId(novel));
    }

    @Test
    void derivesRepeatableDistinctShortIdentifiers() {
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
