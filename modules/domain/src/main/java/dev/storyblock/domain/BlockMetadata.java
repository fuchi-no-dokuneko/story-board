package dev.storyblock.domain;

import java.util.Map;
import java.util.Set;

public record BlockMetadata(Map<String, Object> fields) {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "time",
            "location",
            "weather",
            "speech",
            "actions",
            "presence_events",
            "pov",
            "narrative_mode",
            "provenance"
    );

    public BlockMetadata {
        fields = CanonicalValues.freezeMap(fields, "block.meta");
        for (String field : fields.keySet()) {
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new IllegalArgumentException("Unsupported block metadata field: " + field);
            }
        }
    }

    public static BlockMetadata empty() {
        return new BlockMetadata(Map.of());
    }
}
