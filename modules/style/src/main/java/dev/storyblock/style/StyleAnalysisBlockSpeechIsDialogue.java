package dev.storyblock.style;

import java.util.Map;
import java.util.Set;

final class StyleAnalysisBlockSpeechIsDialogue {
    static boolean speechIsDialogue(Object speech) {
        if (!(speech instanceof Map<?, ?> map)) {
            return false;
        }
        String type = StyleAnalysisBlockScalar.scalar(map.get("type"), null);
        if (type == null && map.get("value") instanceof Map<?, ?> nested) {
            type = StyleAnalysisBlockScalar.scalar(nested.get("type"), null);
        }
        return type != null && !java.util.Set.of("none", "narrated").contains(
                type.toLowerCase(java.util.Locale.ROOT)
        );
    }
}
