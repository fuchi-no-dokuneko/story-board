package dev.storyblock.style;

import java.util.Map;

final class StyleAnalysisBlockSpeaker {
    static String speaker(Object speech) {
        if (!(speech instanceof Map<?, ?> map)) {
            return null;
        }
        for (String field : java.util.List.of(
                "speaker_id", "direct_speaker_id", "character_id"
        )) {
            Object direct = map.get(field);
            if (direct instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        Object nested = map.get("value");
        if (nested instanceof Map<?, ?> value) {
            for (String field : java.util.List.of(
                    "speaker_id", "direct_speaker_id", "character_id"
            )) {
                Object direct = value.get(field);
                if (direct instanceof String text && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }
}
