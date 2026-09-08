package dev.storyblock.renderer;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.MetadataValueState;
import dev.storyblock.domain.SceneSeed;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class MetadataResolutionState {
    static final List<String> INHERITABLE_FIELDS = List.of(
            "time", "location", "weather", "pov"
    );

    static final Set<String> EXPLICIT_CONTROL_FIELDS = Set.of("mode", "evidence");
    private static final Map<String, Object> UNKNOWN = Map.of("mode", "unknown");
    private static final Map<String, Object> NOT_APPLICABLE = Map.of("mode", "not_applicable");

    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Set<String> presentCharacterIds = new TreeSet<>();

    private MetadataResolutionState() {
        for (String field : INHERITABLE_FIELDS) {
            values.put(field, UNKNOWN);
        }
    }

    static MetadataResolutionState fromSceneSeed(SceneSeed seed) {
        MetadataResolutionState state = new MetadataResolutionState();
        if (seed == null) {
            return state;
        }

        Map<String, Object> fields = seed.fields();
        for (String field : List.of("time", "location", "weather")) {
            if (fields.containsKey(field)) {
                state.applyObservation(field, fields.get(field), "scene.initial_meta." + field);
            }
        }
        state.presentCharacterIds.addAll(MetadataResolutionStateStrings.strings(
                fields.get("present_character_ids"),
                "scene.initial_meta.present_character_ids"
        ));
        return state;
    }

    List<Map<String, Object>> apply(BlockMetadata metadata) {
        Map<String, Object> fields = metadata.fields();
        for (String field : INHERITABLE_FIELDS) {
            if (fields.containsKey(field)) {
                applyObservation(field, fields.get(field), "block.meta." + field);
            }
        }

        List<Map<String, Object>> events = MetadataResolutionStatePresenceEvents.presenceEvents(fields.get("presence_events"));
        for (Map<String, Object> event : events) {
            String type = MetadataResolutionStateRequiredString.requiredString(event.get("type"), "presence event type");
            String characterId = MetadataResolutionStateRequiredString.requiredString(
                    event.get("character_id"), "presence event character_id"
            );
            switch (type) {
                case "enter" -> presentCharacterIds.add(characterId);
                case "exit" -> presentCharacterIds.remove(characterId);
                default -> throw new IllegalArgumentException(
                        "Unsupported presence event type: " + type
                );
            }
        }
        return events;
    }

    Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>(values);
        snapshot.put("present_character_ids", List.copyOf(presentCharacterIds));
        return CanonicalValues.freezeMap(snapshot, "resolved_state");
    }

    private void applyObservation(String field, Object raw, String path) {
        if (!(raw instanceof Map<?, ?> observation)
                || !(observation.get("mode") instanceof String canonicalMode)) {
            throw new IllegalArgumentException(path + " must declare a metadata mode");
        }

        MetadataValueState mode = MetadataValueState.fromCanonicalName(canonicalMode);
        switch (mode) {
            case EXPLICIT -> values.put(field, MetadataResolutionStateExplicitValue.explicitValue(observation, path));
            case INHERITED -> {
                // Retain the resolved value, including unknown or not-applicable.
            }
            case UNKNOWN -> values.put(field, UNKNOWN);
            case NOT_APPLICABLE -> values.put(field, NOT_APPLICABLE);
        }
    }

}
