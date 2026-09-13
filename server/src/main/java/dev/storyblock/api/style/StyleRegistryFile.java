package dev.storyblock.api.style;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;

final class StyleRegistryFile {
    static List<StyleDefinition> read(Path yaml) throws IOException {
        var options = new LoaderOptions(); options.setAllowDuplicateKeys(false);
        Object document;
        try (var input = Files.newInputStream(yaml)) { document = new Yaml(new SafeConstructor(options)).load(input); }
        if (!(document instanceof Map<?, ?> root) || !(root.get("styles") instanceof List<?> styles))
            throw new IllegalArgumentException("Style registry requires a styles list");
        var result = new ArrayList<StyleDefinition>();
        var ids = new HashSet<String>();
        for (Object value : styles) {
            if (!(value instanceof Map<?, ?> style)) throw new IllegalArgumentException("Invalid style entry");
            String id = text(style, "id");
            if (!id.matches("[a-z][a-z0-9_-]{0,63}") || !ids.add(id))
                throw new IllegalArgumentException("Style IDs must be distinct slugs");
            if (!(style.get("sources_dir") instanceof List<?> dirs) || dirs.isEmpty())
                throw new IllegalArgumentException("sources_dir requires at least one directory");
            var paths = new ArrayList<Path>();
            for (Object dir : dirs) {
                if (!(dir instanceof String path) || path.isBlank()) throw new IllegalArgumentException("Invalid source directory");
                Path source = Path.of(path);
                paths.add(source.isAbsolute() ? source : yaml.getParent().resolve(source).normalize());
            }
            result.add(new StyleDefinition(id, text(style, "name"), text(style, "description"),
                    text(style, "language"), List.copyOf(paths)));
        }
        return List.copyOf(result);
    }

    private static String text(Map<?, ?> map, String name) {
        if (!(map.get(name) instanceof String value) || value.isBlank())
            throw new IllegalArgumentException("Style requires " + name);
        return value;
    }
}
