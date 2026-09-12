package dev.storyblock.renderer;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.CanonicalValues;
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
    static final Map<String, Object> UNKNOWN = Map.of("mode", "unknown");
    static final Map<String, Object> NOT_APPLICABLE = Map.of("mode", "not_applicable");

    final Map<String, Object> values = new LinkedHashMap<>();
    final Set<String> presentCharacterIds = new TreeSet<>();

    MetadataResolutionState() {
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
        return MetadataResolutionStateApplyAction.apply(this, metadata);
    }

    Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>(values);
        snapshot.put("present_character_ids", List.copyOf(presentCharacterIds));
        return CanonicalValues.freezeMap(snapshot, "resolved_state");
    }

    void applyObservation(String field, Object raw, String path) {
        MetadataResolutionStateApplyObservationAction.applyObservation(this, field, raw, path);
    }

}
