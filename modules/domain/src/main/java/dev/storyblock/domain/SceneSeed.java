package dev.storyblock.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SceneSeed(Map<String, Object> fields) {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "time",
            "location",
            "weather",
            "present_character_ids"
    );

    public SceneSeed {
        fields = CanonicalValues.freezeMap(fields, "scene.initial_meta");
        for (String field : fields.keySet()) {
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new IllegalArgumentException("Unsupported scene seed field: " + field);
            }
        }
        validatePresentCharacters(fields.get("present_character_ids"));
    }

    public static SceneSeed empty() {
        return new SceneSeed(Map.of());
    }

    public static SceneSeed of(
            LocalObservation time,
            LocalObservation location,
            LocalObservation weather,
            List<String> presentCharacterIds
    ) {
        return new SceneSeed(Map.of(
                "time", time.canonicalValue(),
                "location", location.canonicalValue(),
                "weather", weather.canonicalValue(),
                "present_character_ids", List.copyOf(presentCharacterIds)
        ));
    }

    private static void validatePresentCharacters(Object value) {
        if (value == null) {
            return;
        }
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("present_character_ids must be a list");
        }
        Set<String> unique = new HashSet<>();
        for (Object entry : values) {
            if (!(entry instanceof String id) || id.isBlank()) {
                throw new IllegalArgumentException("present_character_ids must contain non-blank strings");
            }
            if (!unique.add(id)) {
                throw new IllegalArgumentException("present_character_ids must be unique");
            }
        }
    }
}
