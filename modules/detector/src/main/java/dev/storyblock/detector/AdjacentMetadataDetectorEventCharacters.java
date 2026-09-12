package dev.storyblock.detector;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class AdjacentMetadataDetectorEventCharacters {
    static Set<String> eventCharacters(
            List<Map<String, Object>> events,
            String type
    ) {
        Set<String> characters = new TreeSet<>();
        for (Map<String, Object> event : events) {
            if (type.equals(event.get("type"))
                    && event.get("character_id") instanceof String characterId) {
                characters.add(characterId);
            }
        }
        return characters;
    }
}
