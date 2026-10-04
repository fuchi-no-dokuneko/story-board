package dev.storyblock.cache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConcurrentCacheTest {
    @Test void simultaneousReadersShareOneLoadWithoutBlockingOtherKeys() throws Exception {
        var cache = new MemoCache<String,String>(2);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var start = new CountDownLatch(1); var ready = new CountDownLatch(24);
        var calls = new AtomicInteger();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<String>>();
            for (int i=0;i<24;i++) futures.add(pool.submit(() -> {
                ready.countDown(); start.await();
                return cache.get("novel",() -> {
                    calls.incrementAndGet(); entered.countDown();
                    try { assertTrue(release.await(5,TimeUnit.SECONDS)); }
                    catch (InterruptedException e) { throw new RuntimeException(e); }
                    return "revision";
                });
            }));
            try {
                assertTrue(ready.await(5,TimeUnit.SECONDS)); start.countDown();
                assertTrue(entered.await(5,TimeUnit.SECONDS));
                assertEquals("other",pool.submit(() -> cache.get("other",() -> "other")).get(2,TimeUnit.SECONDS));
            } finally { start.countDown(); release.countDown(); }
            for (var future : futures) assertEquals("revision",future.get(5,TimeUnit.SECONDS));
            assertEquals(1,calls.get()); assertEquals(23,cache.snapshot().hits());
            assertEquals(2,cache.snapshot().loads());
        }
    }
}
