package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.PRESENCE;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.FactKey;

final class RewriteProtectedFactExtractorAddPresenceEvents {
    static void addPresenceEvents(Map<FactKey, Integer> facts, Object value) {
        if (!(value instanceof List<?> events)) {
            return;
        }
        for (Object entry : events) {
            if (!(entry instanceof Map<?, ?> event)) {
                continue;
            }
            Map<String, Object> identity = new LinkedHashMap<>();
            identity.put("character_id", event.get("character_id"));
            identity.put("type", event.get("type"));
            RewriteProtectedFactExtractorAdd.add(facts, ProtectedFactKind.PRESENCE, CanonicalJson.string(identity), 1);
        }
    }
}
