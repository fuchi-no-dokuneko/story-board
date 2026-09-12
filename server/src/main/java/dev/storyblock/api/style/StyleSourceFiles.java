package dev.storyblock.api.style;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;

final class StyleSourceFiles {
    record Stamp(Path path, long modifiedNanos, long size) {}

    static List<Stamp> inspect(StyleDefinition definition) throws IOException {
        var paths = new TreeSet<Path>();
        for (Path directory : definition.sources()) {
            if (!Files.isDirectory(directory)) throw new IOException("Source directory is unavailable: " + directory);
            try (var files = Files.walk(directory.toRealPath(), FileVisitOption.FOLLOW_LINKS)) {
                for (Path path : files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt")).toList())
                    paths.add(path.toRealPath());
            }
        }
        if (paths.isEmpty()) throw new IOException("No TXT reference files for " + definition.id());
        var result = new ArrayList<Stamp>();
        for (Path path : paths) {
            var attr = Files.readAttributes(path, BasicFileAttributes.class);
            result.add(new Stamp(path, attr.lastModifiedTime().to(TimeUnit.NANOSECONDS), attr.size()));
        }
        return List.copyOf(result);
    }
}
