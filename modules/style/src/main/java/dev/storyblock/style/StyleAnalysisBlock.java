package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeScene;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record StyleAnalysisBlock(
        Ids.SceneId sceneId,
        NarrativeBlock block,
        StyleStratumKind stratumKind,
        String speakerId,
        String pov,
        String narrativeMode,
        String intentionalStyleShiftReason
) {
    static final Pattern SUBJECT = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");
    static final Set<String> FIELDS = Set.of(
            "scene_id", "block", "stratum_kind", "speaker_id", "pov",
            "narrative_mode", "intentional_style_shift_reason"
    );
    static final Set<String> BLOCK_FIELDS = Set.of(
            "id", "block_version_id", "order_key", "text", "metadata", "extensions"
    );

    public StyleAnalysisBlock {
        Objects.requireNonNull(sceneId, "sceneId");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(stratumKind, "stratumKind");
        if (speakerId != null && !SUBJECT.matcher(speakerId).matches()) {
            throw new IllegalArgumentException("Style analysis speaker ID is invalid");
        }
        pov = StyleAnalysisBlockNormalizedLabel.normalizedLabel(pov, "pov");
        narrativeMode = StyleAnalysisBlockNormalizedLabel.normalizedLabel(narrativeMode, "narrativeMode");
        if (intentionalStyleShiftReason != null
                && (intentionalStyleShiftReason.isBlank()
                || intentionalStyleShiftReason.length() > 500)) {
            throw new IllegalArgumentException("Intentional style shift reason is invalid");
        }
    }

    public static StyleAnalysisBlock from(
            NarrativeScene scene,
            NarrativeBlock block
    ) {
        return StyleAnalysisBlockFromFactory.from(scene, block);
    }

    static boolean isDialogue(dev.storyblock.domain.NarrativeText block) {
        return StyleAnalysisBlockIsDialogueFactory.isDialogue(block);
    }

    public static StyleAnalysisBlock fromCanonical(Map<String, Object> value) {
        return StyleAnalysisBlockFromCanonicalFactory.fromCanonical(value);
    }

    public Map<String, Object> canonicalValue() {
        return StyleAnalysisBlockCanonicalValueAction.canonicalValue(this);
    }

}
