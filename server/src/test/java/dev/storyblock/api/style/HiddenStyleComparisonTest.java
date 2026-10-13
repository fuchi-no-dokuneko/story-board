package dev.storyblock.api.style;

import dev.storyblock.api.http.*;
import dev.storyblock.application.NovelReadCache;
import dev.storyblock.storage.sqlite.SqliteRevisionStore;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HiddenStyleComparisonTest {
    @TempDir Path root;
    @Test void everyComparisonChannelEqualsNotesRemoved() throws Exception {
        var directory = Files.createDirectories(root.resolve("reference"));
        Files.writeString(directory.resolve("text.txt"),"夜雨落在窗前。她聽著遠處的鐘聲。這是很長的一夜。");
        var definition = new StyleDefinition("reference","Reference","Authored fixture","zh-Hant",List.of(directory));
        var benchmark = StyleBenchmark.calculate(definition,StyleSourceFiles.inspect(definition));
        var full = HiddenHttpFixture.revision(); var plain = HiddenHttpFixture.withoutHidden(full);
        try (var source = SqliteRevisionStore.open(root.resolve("source.db"));
             var clean = SqliteRevisionStore.open(root.resolve("clean.db"))) {
            source.createNovel(full,CacheRevisionFixture.hash(full));clean.createNovel(plain,CacheRevisionFixture.hash(plain));
            var actual = StyleComparisonCalculation.calculate(full.novel().id().value(),
                    new NovelReadCache(source).read(full.novel().id(),full.id()),List.of(benchmark));
            var expected = StyleComparisonCalculation.calculate(plain.novel().id().value(),
                    new NovelReadCache(clean).read(plain.novel().id(),plain.id()),List.of(benchmark));
            assertEquals(expected.get("comparisons"),actual.get("comparisons"));
            assertFalse(dev.storyblock.contracts.CanonicalJson.string(actual).contains(HiddenHttpFixture.SENTINEL));
        }
    }
}
