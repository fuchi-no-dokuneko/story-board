package dev.storyblock.style;

import dev.storyblock.domain.NarrativeBlock;
import java.util.Map;
import java.util.Objects;

final class StyleAnalysisBlockIsDialogueFactory {
    static boolean isDialogue(NarrativeBlock block)  {
        Objects.requireNonNull(block, "block");
        Map<String, Object> metadata = block.metadata().fields();
        String mode = StyleAnalysisBlockScalar.scalar(metadata.get("narrative_mode"), null);
        return "dialogue".equalsIgnoreCase(mode)
                || StyleAnalysisBlockSpeaker.speaker(metadata.get("speech")) != null
                || StyleAnalysisBlockSpeechIsDialogue.speechIsDialogue(metadata.get("speech"))
                || StyleAnalysisBlockContainsDialogueMarks.containsDialogueMarks(block.text());
    }
}
