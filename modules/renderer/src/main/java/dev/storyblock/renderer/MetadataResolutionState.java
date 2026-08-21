package dev.storyblock.renderer;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.MetadataValueState;
import dev.storyblock.domain.SceneSeed;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class MetadataResolutionState {
    static final List<String> INHERITABLE_FIELDS = List.of(
            "time", "location", "weather", "pov"
    );

    private static final Set<String> EXPLICIT_CONTROL_FIELDS = Set.of("mode", "evidence");
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
        state.presentCharacterIds.addAll(strings(
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

        List<Map<String, Object>> events = presenceEvents(fields.get("presence_events"));
        for (Map<String, Object> event : events) {
            String type = requiredString(event.get("type"), "presence event type");
            String characterId = requiredString(
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
            case EXPLICIT -> values.put(field, explicitValue(observation, path));
            case INHERITED -> {
                // Retain the resolved value, including unknown or not-applicable.
            }
            case UNKNOWN -> values.put(field, UNKNOWN);
            case NOT_APPLICABLE -> values.put(field, NOT_APPLICABLE);
        }
    }

    private static Object explicitValue(Map<?, ?> observation, String path) {
        if (observation.containsKey("value")) {
            Object value = observation.get("value");
            if (value == null) {
                throw new IllegalArgumentException(path + " explicit mode requires a value");
            }
            return CanonicalValues.freeze(value, path + ".value");
        }

        Map<String, Object> inlineValue = new LinkedHashMap<>();
        for (Map.Entry<?, ?> field : observation.entrySet()) {
            if (!(field.getKey() instanceof String key)) {
                throw new IllegalArgumentException(path + " contains a non-string key");
            }
            if (!EXPLICIT_CONTROL_FIELDS.contains(key)) {
                inlineValue.put(key, field.getValue());
            }
        }
        if (inlineValue.isEmpty()) {
            throw new IllegalArgumentException(path + " explicit mode requires a value");
        }
        return CanonicalValues.freezeMap(inlineValue, path + ".value");
    }

    private static List<Map<String, Object>> presenceEvents(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> entries)) {
            throw new IllegalArgumentException("block.meta.presence_events must be a list");
        }

        List<Map<String, Object>> events = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            Object entry = entries.get(index);
            if (!(entry instanceof Map<?, ?> event)) {
                throw new IllegalArgumentException(
                        "block.meta.presence_events[" + index + "] must be an object"
                );
            }
            Map<String, Object> typed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> field : event.entrySet()) {
                if (!(field.getKey() instanceof String key)) {
                    throw new IllegalArgumentException(
                            "block.meta.presence_events[" + index + "] contains a non-string key"
                    );
                }
                typed.put(key, field.getValue());
            }
            events.add(CanonicalValues.freezeMap(
                    typed, "block.meta.presence_events[" + index + "]"
            ));
        }
        return List.copyOf(events);
    }

    private static List<String> strings(Object raw, String path) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> entries)) {
            throw new IllegalArgumentException(path + " must be a list");
        }
        Set<String> sorted = new TreeSet<>();
        for (Object entry : entries) {
            sorted.add(requiredString(entry, path + " entry"));
        }
        return List.copyOf(sorted);
    }

    private static String requiredString(Object value, String label) {
        if (!(value instanceof String string) || string.isBlank()) {
            throw new IllegalArgumentException(label + " must be a non-blank string");
        }
        return string;
    }
}
