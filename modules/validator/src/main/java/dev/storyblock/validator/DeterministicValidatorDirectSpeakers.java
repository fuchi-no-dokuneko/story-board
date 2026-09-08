package dev.storyblock.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DeterministicValidatorDirectSpeakers {
    static Set<String> directSpeakers(Object speechValue) {
        if (!(speechValue instanceof Map<?, ?> speech)) {
            return Set.of();
        }
        Set<String> speakers = new HashSet<>();
        DeterministicValidatorAddStrings.addStrings(speakers, speech.get("direct_speaker_id"));
        DeterministicValidatorAddStrings.addStrings(speakers, speech.get("direct_speaker_ids"));
        Object turns = speech.get("turns");
        if (turns instanceof List<?> values) {
            for (Object value : values) {
                if (value instanceof Map<?, ?> turn
                        && "direct".equals(turn.get("channel"))) {
                    DeterministicValidatorAddStrings.addStrings(speakers, turn.get("speaker_id"));
                }
            }
        }
        return Set.copyOf(speakers);
    }
}
