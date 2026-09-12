package dev.storyblock.rewrite.policy;

import java.util.List;
import java.util.Map;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.FactKey;

final class RewriteProtectedFactExtractorAddSpeakers {
    static void addSpeakers(Map<FactKey, Integer> facts, Object value) {
        if (!(value instanceof Map<?, ?> speech)) {
            return;
        }
        RewriteProtectedFactExtractorAddStringValues.addStringValues(facts, ProtectedFactKind.SPEAKER, speech.get(
                "direct_speaker_id"
        ));
        RewriteProtectedFactExtractorAddStringValues.addStringValues(facts, ProtectedFactKind.SPEAKER, speech.get(
                "direct_speaker_ids"
        ));
        RewriteProtectedFactExtractorAddStringValues.addStringValues(facts, ProtectedFactKind.SPEAKER, speech.get("speaker_id"));
        if (speech.get("turns") instanceof List<?> turns) {
            for (Object valueEntry : turns) {
                if (valueEntry instanceof Map<?, ?> turn
                        && "direct".equals(turn.get("channel"))) {
                    RewriteProtectedFactExtractorAddStringValues.addStringValues(
                            facts, ProtectedFactKind.SPEAKER, turn.get("speaker_id")
                    );
                }
            }
        }
    }
}
