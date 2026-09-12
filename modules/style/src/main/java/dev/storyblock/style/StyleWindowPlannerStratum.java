package dev.storyblock.style;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class StyleWindowPlannerStratum {
    static StyleStratum stratum(
            StyleStratumKind kind,
            List<StyleAnalysisBlock> blocks
    ) {
        if (kind == StyleStratumKind.NARRATION) {
            return StyleStratum.narration();
        }
        Set<String> speakers = new LinkedHashSet<>();
        boolean missingSpeaker = false;
        for (StyleAnalysisBlock block : blocks) {
            if (block.speakerId() == null) {
                missingSpeaker = true;
            } else {
                speakers.add(block.speakerId());
            }
        }
        return !missingSpeaker && speakers.size() == 1
                ? StyleStratum.dialogue(speakers.iterator().next())
                : StyleStratum.dialogue();
    }
}
