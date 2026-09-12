package dev.storyblock.api.style;

import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StyleLibraryRefreshTest {
    @TempDir Path temporary;
    @Test void refreshTracksExternalSourcesAndNestedFileChanges() throws Exception {
        Path root = Files.createDirectory(temporary.resolve("server root"));
        Path sources = Files.createDirectories(temporary.resolve("external references/nested"));
        Path text = sources.resolve("source.txt");
        Files.writeString(text, "夜雨打在窗上。她靜靜等候。\n這一段沒有句末標點");
        Path yaml = root.resolve("styles.yaml");
        Files.writeString(yaml, registry(sources.getParent()));
        var library = new StyleLibrary(root, "styles.yaml");
        library.refresh();
        var first = library.snapshot().benchmarks().get("lyrical");
        assertNotNull(first);
        library.refresh();
        assertSame(first, library.snapshot().benchmarks().get("lyrical"));
        var directoryTime = Files.getLastModifiedTime(sources);
        Files.writeString(text, "警笛響了！快跑！他衝出大門！");
        Files.setLastModifiedTime(text, FileTime.fromMillis(System.currentTimeMillis() + 1000));
        assertEquals(directoryTime, Files.getLastModifiedTime(sources));
        library.refresh();
        var changed = library.snapshot().benchmarks().get("lyrical");
        assertNotEquals(first.features().sourceHash(), changed.features().sourceHash());
        Path added = sources.resolve("added.TXT"); Files.writeString(added, "風停了。");
        library.refresh();
        assertEquals(2, library.snapshot().benchmarks().get("lyrical").sources().size());
        Files.delete(text); library.refresh();
        assertEquals(1, library.snapshot().benchmarks().get("lyrical").sources().size());
        Files.delete(added); library.refresh();
        assertTrue(library.snapshot().errors().containsKey("lyrical"));
        Files.writeString(yaml, "schema_version: 1\nstyles: []\n"); library.refresh();
        assertTrue(library.snapshot().benchmarks().isEmpty());
        library.stop();
    }

    static String registry(Path sources) {
        return "schema_version: 1\nstyles:\n  - id: lyrical\n    name: 抒情敘事\n"
                + "    description: 環境與內心描寫\n    language: zh-Hant\n    sources_dir:\n"
                + "      - '" + sources + "'\n";
    }
}
