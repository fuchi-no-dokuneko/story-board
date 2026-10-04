package dev.storyblock.api.http;

import dev.storyblock.application.NovelReadCache;
import dev.storyblock.api.quality.*;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.nio.file.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class QualityCacheTest {
    @TempDir Path root;
    @Test void reportsReuseIdenticalInputsAndRefreshForNamesOrRevision() throws Exception {
        try (var store = SqliteRevisionStore.open(root.resolve("quality.db"))) {
            var base = CacheRevisionFixture.create(store); var id = base.novel().id().value();
            var hash = CacheRevisionFixture.hash(base);
            var reference = new QualityReference(root.resolve("quality.json").toString());
            var service = new NovelQualityService(new NovelReadCache(store),reference);
            var first = service.analyze(id,null,hash,Set.of());
            assertSame(first,service.analyze(id,base.id().value(),hash,Set.of()));
            assertNotSame(first,service.analyze(id,null,hash,Set.of("山谷")));
            assertThrows(IllegalArgumentException.class,() -> service.analyze(id,null,"stale",Set.of()));
            var changed = CacheRevisionFixture.commit(store,base);
            var next = service.analyze(id,null,CacheRevisionFixture.hash(changed),Set.of());
            assertNotSame(first,next); assertEquals(changed.id().value(),next.get("revision_id"));
            var historical = service.analyze(id,base.id().value(),hash,Set.of());
            assertEquals(first,historical);
            var restarted = new NovelQualityService(new NovelReadCache(store),reference);
            var afterRestart = restarted.analyze(id,base.id().value(),hash,Set.of());
            assertEquals(first,afterRestart); assertNotSame(historical,afterRestart);
        }
    }
}
