package dev.storyblock.storage.sqlite;

import dev.storyblock.domain.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ShortIdPersistenceTest {
    @TempDir Path directory;
    @Test void collisionRetriesAndReopenKeepTheSameAllocation() throws Exception {
        Path path = directory.resolve("ids.db");
        String key = "blv\0op_for_edit\0replacement";
        String collision = ShortIds.candidate("blv", key, 0);
        String allocated;
        try (var store = SqliteRevisionStore.open(path)) {
            store.write(connection -> {
                SqliteIdentityClaim.claim(connection, collision, "existing-block", "original-text");
                return null;
            });
            var allocator = new SqliteShortIds(store);
            allocated = allocator.allocate("blv", key);
            assertNotEquals(collision, allocated);
            assertEquals(allocated, allocator.allocate("blv", key));
        }
        try (var reopened = SqliteRevisionStore.open(path)) {
            assertEquals(allocated, new SqliteShortIds(reopened).allocate("blv", key));
        }
    }

    @Test void concurrentRetriesShareAnIdAndDifferentRequestsStayUnique() throws Exception {
        try (var store = SqliteRevisionStore.open(directory.resolve("concurrent.db"));
             var executor = Executors.newFixedThreadPool(8)) {
            var allocator = new SqliteShortIds(store);
            var tasks = new ArrayList<Callable<String>>();
            for (int i = 0; i < 40; i++) tasks.add(() -> allocator.allocate("blk", "same-insert"));
            var same = new HashSet<String>();
            for (var result : executor.invokeAll(tasks)) same.add(result.get());
            assertEquals(1, same.size());
            tasks.clear();
            for (int i = 0; i < 40; i++) {
                String key = "insert-" + i;
                tasks.add(() -> allocator.allocate("blk", key));
            }
            var distinct = new HashSet<String>();
            for (var result : executor.invokeAll(tasks)) assertTrue(distinct.add(result.get()));
            assertEquals(40, distinct.size());
        }
    }
}
