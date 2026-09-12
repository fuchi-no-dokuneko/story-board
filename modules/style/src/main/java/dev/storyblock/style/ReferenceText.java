package dev.storyblock.style;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.NarrativeText;
import java.util.Map;

public record ReferenceText(String text) implements NarrativeText {
    public ReferenceText {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Reference text is empty");
    }
    public BlockMetadata metadata() { return BlockMetadata.empty(); }
    public Map<String, Object> extensions() { return Map.of(); }
}
