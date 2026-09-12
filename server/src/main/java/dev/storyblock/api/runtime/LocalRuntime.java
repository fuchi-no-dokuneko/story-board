package dev.storyblock.api.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class LocalRuntime {
    private LocalRuntime() {}

    static Path directory(String relative) throws IOException {
        Path root = Path.of(System.getProperty("storyblock.root", ".")).toRealPath();
        Path directory = contained(root.resolve(".local/storyblock").resolve(relative));
        Files.createDirectories(directory);
        return directory;
    }

    static Path contained(Path candidate) throws IOException {
        Path root = Path.of(System.getProperty("storyblock.root", ".")).toRealPath();
        Path directory = candidate.toAbsolutePath().normalize();
        Path existing = directory;
        while (!Files.exists(existing)) existing = existing.getParent();
        if (!directory.startsWith(root) || !existing.toRealPath().startsWith(root)) {
            throw new IOException("Runtime files must remain inside the repository");
        }
        return directory;
    }
}
