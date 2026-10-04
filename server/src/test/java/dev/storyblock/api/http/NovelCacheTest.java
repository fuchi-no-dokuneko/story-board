package dev.storyblock.api.http;

import dev.storyblock.application.*;
import dev.storyblock.contracts.*;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.nio.file.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class NovelCacheTest {
    @TempDir Path root;
    @Test void catalogAndDetailReuseRevisionUntilAnotherConnectionCommits() throws Exception {
        var db = root.resolve("novels.db");
        try (var store = SqliteRevisionStore.open(db); var writer = SqliteRevisionStore.open(db)) {
            var base = CacheRevisionFixture.create(store); var id = base.novel().id();
            var reads = new NovelReadCache(store); var catalog = new NovelCatalogService(store,reads);
            var first = catalog.read(id);
            assertSame(first,catalog.read(id)); assertSame(first.novel(),catalog.list("").getFirst());
            assertEquals(1,reads.snapshot().loads());
            var changed = CacheRevisionFixture.commit(writer,base);
            var next = catalog.read(id);
            assertNotSame(first,next); assertEquals(changed.id().value(),next.revision().canonicalContent().get("revision_id"));
            assertEquals(changed.id(),next.novel().headRevisionId()); assertEquals(2,reads.snapshot().loads());
            assertEquals(base.id(),reads.read(id,base.id()).revision().manifest().id());
            CacheRevisionFixture.create(writer); assertEquals(2,catalog.list("").size());
        }
    }
    @Test void missingNovelDoesNotCacheFailureAndImportedNovelAppears() throws Exception {
        try (var store = SqliteRevisionStore.open(root.resolve("target.db"));
             var source = SqliteRevisionStore.open(root.resolve("source.db"))) {
            var base = CacheRevisionFixture.create(source); var reads = new NovelReadCache(store);
            assertThrows(RuntimeException.class,() -> reads.read(base.novel().id(),null));
            new CanonicalTransferService(store).importDocument(CanonicalExportFormat.PACKAGE,
                    new CanonicalTransferService(source).exportPackage(base.novel().id()),"import",Instant.now());
            assertEquals(base.id(),reads.read(base.novel().id(),null).revision().manifest().id());
            assertEquals(1,reads.snapshot().loads());
        }
    }
}
