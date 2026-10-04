package dev.storyblock.api.style;

import dev.storyblock.application.NovelReadCache;
import dev.storyblock.api.http.*;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StyleCacheTest {
    @TempDir Path root;
    @Test void cachedComparisonsTrackReferenceRefreshAndUnavailability() throws Exception {
        var sources = Files.createDirectories(root.resolve("source/a"));
        var text = sources.resolve("reference.txt");
        Files.writeString(text,"夜雨落在窗前。她聽著遠處的鐘聲。這是很長的一夜。");
        Files.writeString(root.resolve("styles.yaml"),StyleLibraryRefreshTest.registry(sources.getParent()));
        var library = new StyleLibrary(root,"styles.yaml"); library.refresh();
        try (var store = SqliteRevisionStore.open(root.resolve("style.db"))) {
            var base = CacheRevisionFixture.create(store); var id = base.novel().id().value();
            var hash = CacheRevisionFixture.hash(base); var ids = List.of("lyrical");
            var service = new StyleComparisonService(library,new NovelReadCache(store));
            var first = service.compare(id,null,hash,ids);
            assertSame(first,service.compare(id,null,hash,ids));
            library.refresh(); assertSame(first,service.compare(id,null,hash,ids));
            assertThrows(IllegalArgumentException.class,() -> service.compare(id,null,"stale",ids));
            Files.writeString(text,"快走！他跑了！警笛響了！一切突然停了。");
            Files.setLastModifiedTime(text,FileTime.fromMillis(System.currentTimeMillis()+1000));
            library.refresh(); assertNotSame(first,service.compare(id,null,hash,ids));
            Files.delete(text); library.refresh();
            assertThrows(ApiFailureException.class,() -> service.compare(id,null,hash,ids));
            Files.writeString(text,"再度亮起的燈。路過的人停下了腳步。"); library.refresh();
            var recovered = service.compare(id,null,hash,ids);
            assertSame(recovered,service.compare(id,null,hash,ids));
            var changed = CacheRevisionFixture.commit(store,base);
            assertNotSame(recovered,service.compare(id,null,CacheRevisionFixture.hash(changed),ids));
        } finally { library.stop(); }
    }
}
