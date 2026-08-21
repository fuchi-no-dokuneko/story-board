package dev.storyblock.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalJsonTest {
    record Fixture(String alpha, int number) {
    }

    @Test
    void mapInsertionOrderDoesNotChangeCanonicalBytesOrHash() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("zeta", 2);
        first.put("alpha", 1);
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("alpha", 1);
        second.put("zeta", 2);

        assertEquals("{\"alpha\":1,\"zeta\":2}", CanonicalJson.string(first));
        assertEquals(CanonicalJson.hash(first), CanonicalJson.hash(second));
    }

    @Test
    void parserRejectsUnknownFields() {
        byte[] json = "{\"alpha\":\"a\",\"number\":1,\"unknown\":true}".getBytes();
        assertThrows(RuntimeException.class, () -> CanonicalJson.parse(json, Fixture.class));
    }
}
