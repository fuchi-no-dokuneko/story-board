package dev.storyblock.validator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class DeterministicValidatorApplyEvents {
    static Set<String> applyEvents(
            Set<String> presentBefore,
            List<Map<String, Object>> events
    ) {
        Set<String> present = new TreeSet<>(presentBefore);
        for (Map<String, Object> event : events) {
            Object character = event.get("character_id");
            if (!(character instanceof String characterId)) {
                continue;
            }
            if ("enter".equals(event.get("type"))) {
                present.add(characterId);
            } else if ("exit".equals(event.get("type"))) {
                present.remove(characterId);
            }
        }
        return Set.copyOf(present);
    }
}
