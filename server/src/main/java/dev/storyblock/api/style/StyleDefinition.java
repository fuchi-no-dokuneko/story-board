package dev.storyblock.api.style;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record StyleDefinition(String id, String name, String description, String language, List<Path> sources) {
    public Map<String, Object> publicValue() {
        return Map.of("id", id, "name", name, "description", description, "language", language);
    }
}
