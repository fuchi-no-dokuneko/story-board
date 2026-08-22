package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.OrderKey;
import java.util.LinkedHashMap;
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
    private static final Pattern SUBJECT = Pattern.compile("[A-Za-z0-9._:@-]{1,128}");
    private static final Set<String> FIELDS = Set.of(
            "scene_id", "block", "stratum_kind", "speaker_id", "pov",
            "narrative_mode", "intentional_style_shift_reason"
    );
    private static final Set<String> BLOCK_FIELDS = Set.of(
            "id", "block_version_id", "order_key", "text", "metadata", "extensions"
    );

    public StyleAnalysisBlock {
        Objects.requireNonNull(sceneId, "sceneId");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(stratumKind, "stratumKind");
        if (speakerId != null && !SUBJECT.matcher(speakerId).matches()) {
            throw new IllegalArgumentException("Style analysis speaker ID is invalid");
        }
        pov = normalizedLabel(pov, "pov");
        narrativeMode = normalizedLabel(narrativeMode, "narrativeMode");
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
        if (!scene.blocks().contains(block)) {
            throw new IllegalArgumentException("Style block does not belong to its scene");
        }
        Map<String, Object> metadata = block.metadata().fields();
        String mode = scalar(metadata.get("narrative_mode"), null);
        String speaker = speaker(metadata.get("speech"));
        boolean dialogue = isDialogue(block);
        if (mode == null) {
            mode = dialogue ? "dialogue" : "narration";
        }
        String shift = scalar(
                scene.extensions().get("intentional_style_shift_reason"), null
        );
        return new StyleAnalysisBlock(
                scene.id(),
                block,
                dialogue ? StyleStratumKind.DIALOGUE : StyleStratumKind.NARRATION,
                speaker,
                scalar(metadata.get("pov"), "unknown"),
                mode,
                shift
        );
    }

    static boolean isDialogue(NarrativeBlock block) {
        Objects.requireNonNull(block, "block");
        Map<String, Object> metadata = block.metadata().fields();
        String mode = scalar(metadata.get("narrative_mode"), null);
        return "dialogue".equalsIgnoreCase(mode)
                || speaker(metadata.get("speech")) != null
                || speechIsDialogue(metadata.get("speech"))
                || containsDialogueMarks(block.text());
    }

    public static StyleAnalysisBlock fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_analysis_block");
        Map<String, Object> block = StyleCanonical.object(
                value.get("block"), "style_analysis_block.block"
        );
        StyleCanonical.requireKeys(block, BLOCK_FIELDS, "style_analysis_block.block");
        return new StyleAnalysisBlock(
                new Ids.SceneId(StyleCanonical.string(
                        value, "scene_id", "style_analysis_block"
                )),
                new NarrativeBlock(
                        new Ids.BlockId(StyleCanonical.string(
                                block, "id", "style_analysis_block.block"
                        )),
                        new Ids.BlockVersionId(StyleCanonical.string(
                                block, "block_version_id", "style_analysis_block.block"
                        )),
                        new OrderKey(StyleCanonical.string(
                                block, "order_key", "style_analysis_block.block"
                        )),
                        StyleCanonical.string(
                                block, "text", "style_analysis_block.block"
                        ),
                        new BlockMetadata(StyleCanonical.object(
                                block.get("metadata"), "style_analysis_block.block.metadata"
                        )),
                        StyleCanonical.object(
                                block.get("extensions"), "style_analysis_block.block.extensions"
                        )
                ),
                StyleStratumKind.fromCanonicalName(StyleCanonical.string(
                        value, "stratum_kind", "style_analysis_block"
                )),
                StyleCanonical.optionalString(
                        value, "speaker_id", "style_analysis_block"
                ),
                StyleCanonical.string(value, "pov", "style_analysis_block"),
                StyleCanonical.string(
                        value, "narrative_mode", "style_analysis_block"
                ),
                StyleCanonical.optionalString(
                        value,
                        "intentional_style_shift_reason",
                        "style_analysis_block"
                )
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> serializedBlock = new LinkedHashMap<>();
        serializedBlock.put("block_version_id", block.versionId().value());
        serializedBlock.put("extensions", block.extensions());
        serializedBlock.put("id", block.id().value());
        serializedBlock.put("metadata", block.metadata().fields());
        serializedBlock.put("order_key", block.orderKey().value());
        serializedBlock.put("text", block.text());

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block", serializedBlock);
        value.put("intentional_style_shift_reason", intentionalStyleShiftReason);
        value.put("narrative_mode", narrativeMode);
        value.put("pov", pov);
        value.put("scene_id", sceneId.value());
        value.put("speaker_id", speakerId);
        value.put("stratum_kind", stratumKind.canonicalName());
        return CanonicalValues.freezeMap(value, "style_analysis_block");
    }

    private static String normalizedLabel(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Style analysis " + field + " is invalid");
        }
        return value.toLowerCase(java.util.Locale.ROOT);
    }

    private static String speaker(Object speech) {
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

    private static boolean speechIsDialogue(Object speech) {
        if (!(speech instanceof Map<?, ?> map)) {
            return false;
        }
        String type = scalar(map.get("type"), null);
        if (type == null && map.get("value") instanceof Map<?, ?> nested) {
            type = scalar(nested.get("type"), null);
        }
        return type != null && !java.util.Set.of("none", "narrated").contains(
                type.toLowerCase(java.util.Locale.ROOT)
        );
    }

    private static String scalar(Object value, String fallback) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        if (value instanceof Map<?, ?> map) {
            for (String field : java.util.List.of("value", "mode", "label")) {
                Object nested = map.get(field);
                if (nested instanceof String text && !text.isBlank()) {
                    return text;
                }
            }
        }
        return fallback;
    }

    private static boolean containsDialogueMarks(String text) {
        return text.indexOf('"') >= 0 || text.indexOf('\u201c') >= 0
                || text.indexOf('\u201d') >= 0 || text.indexOf('\u300c') >= 0
                || text.indexOf('\u300d') >= 0;
    }
}
