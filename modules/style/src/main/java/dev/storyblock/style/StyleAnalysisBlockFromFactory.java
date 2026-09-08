package dev.storyblock.style;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeScene;
import java.util.Map;

final class StyleAnalysisBlockFromFactory {
    static StyleAnalysisBlock from(NarrativeScene scene, NarrativeBlock block)  {
        if (!scene.blocks().contains(block)) {
            throw new IllegalArgumentException("Style block does not belong to its scene");
        }
        Map<String, Object> metadata = block.metadata().fields();
        String mode = StyleAnalysisBlockScalar.scalar(metadata.get("narrative_mode"), null);
        String speaker = StyleAnalysisBlockSpeaker.speaker(metadata.get("speech"));
        boolean dialogue = StyleAnalysisBlock.isDialogue(block);
        if (mode == null) {
            mode = dialogue ? "dialogue" : "narration";
        }
        String shift = StyleAnalysisBlockScalar.scalar(
                scene.extensions().get("intentional_style_shift_reason"), null
        );
        return new StyleAnalysisBlock(
                scene.id(),
                block,
                dialogue ? StyleStratumKind.DIALOGUE : StyleStratumKind.NARRATION,
                speaker,
                StyleAnalysisBlockScalar.scalar(metadata.get("pov"), "unknown"),
                mode,
                shift
        );
    }
}
