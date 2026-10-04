package dev.storyblock.cache;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemoCacheTest {
    @Test void recentlyReadEntriesSurviveEvictionAndFailuresCanRetry() {
        var cache = new MemoCache<String,String>(2);
        assertEquals("one",cache.get("first",() -> "one"));
        cache.get("second",() -> "two");
        assertEquals("one",cache.get("first",() -> fail("Unexpected reload")));
        cache.get("third",() -> "three");
        assertEquals(2,cache.snapshot().entries());
        assertEquals("reloaded",cache.get("second",() -> "reloaded"));
        assertEquals(4,cache.snapshot().loads());
        var failure = new IllegalStateException("Storage temporarily unavailable");
        assertSame(failure,assertThrows(IllegalStateException.class,
                () -> cache.get("unavailable",() -> { throw failure; })));
        assertEquals("recovered",cache.get("unavailable",() -> "recovered"));
        assertEquals(2,cache.snapshot().entries());
        assertEquals(6,cache.snapshot().loads());
    }
}
