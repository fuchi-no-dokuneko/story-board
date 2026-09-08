package dev.storyblock.renderer;

import dev.storyblock.domain.BlockMetadata;
import java.util.List;
import java.util.Map;

final class MetadataResolutionStateApplyAction {
    static List<Map<String, Object>> apply(MetadataResolutionState self, BlockMetadata metadata)  {
        Map<String, Object> fields = metadata.fields();
        for (String field : MetadataResolutionState.INHERITABLE_FIELDS) {
            if (fields.containsKey(field)) {
                self.applyObservation(field, fields.get(field), "block.meta." + field);
            }
        }

        List<Map<String, Object>> events = MetadataResolutionStatePresenceEvents.presenceEvents(fields.get("presence_events"));
        for (Map<String, Object> event : events) {
            String type = MetadataResolutionStateRequiredString.requiredString(event.get("type"), "presence event type");
            String characterId = MetadataResolutionStateRequiredString.requiredString(
                    event.get("character_id"), "presence event character_id"
            );
            switch (type) {
                case "enter" -> self.presentCharacterIds.add(characterId);
                case "exit" -> self.presentCharacterIds.remove(characterId);
                default -> throw new IllegalArgumentException(
                        "Unsupported presence event type: " + type
                );
            }
        }
        return events;
    }
}
