package dev.storyblock.api.style;

import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StyleRegistryChangeTest {
    @TempDir Path root;
    @Test void yamlTimestampRebuildsAndInvalidChangesRemainVisible() throws Exception {
        Path sources = Files.createDirectory(root.resolve("references"));
        Files.writeString(sources.resolve("text.txt"), "夜風輕輕穿過山谷。她終於聽見回聲。");
        Path yaml = root.resolve("styles.yaml");
        String valid = StyleLibraryRefreshTest.registry(sources);
        Files.writeString(yaml, valid);
        var library = new StyleLibrary(root, "styles.yaml"); library.refresh();
        var original = library.snapshot().benchmarks().get("lyrical");
        Files.setLastModifiedTime(yaml, FileTime.fromMillis(System.currentTimeMillis() + 2000));
        library.refresh();
        assertNotSame(original, library.snapshot().benchmarks().get("lyrical"));
        Files.writeString(yaml, valid.replace("name: 抒情敘事", "name: 山谷敘事")); library.refresh();
        assertEquals("山谷敘事", library.snapshot().definitions().getFirst().name());
        Files.writeString(yaml, "styles: [invalid"); library.refresh();
        assertNotNull(library.snapshot().registryError());
        assertTrue(library.list().containsKey("error"));
        Files.writeString(yaml, valid); library.refresh();
        assertNull(library.snapshot().registryError());
        library.stop();
    }
}
