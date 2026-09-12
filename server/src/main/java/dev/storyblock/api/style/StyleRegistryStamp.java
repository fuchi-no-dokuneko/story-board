package dev.storyblock.api.style;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;

record StyleRegistryStamp(FileTime modified, long size) {
    static StyleRegistryStamp inspect(Path root, Path file) throws IOException {
        if (!file.toRealPath().startsWith(root.toRealPath()))
            throw new IOException("Style YAML must stay inside the repository, including symbolic links");
        var attributes = Files.readAttributes(file, BasicFileAttributes.class);
        return new StyleRegistryStamp(attributes.lastModifiedTime(), attributes.size());
    }
}
