package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleAnalysisBlockCanonicalValueAction {
    static Map<String, Object> canonicalValue(StyleAnalysisBlock self)  {
        Map<String, Object> serializedBlock = new LinkedHashMap<>();
        serializedBlock.put("block_version_id", self.block().versionId().value());
        serializedBlock.put("extensions", self.block().extensions());
        serializedBlock.put("id", self.block().id().value());
        serializedBlock.put("metadata", self.block().metadata().fields());
        serializedBlock.put("order_key", self.block().orderKey().value());
        serializedBlock.put("text", self.block().text());

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("block", serializedBlock);
        value.put("intentional_style_shift_reason", self.intentionalStyleShiftReason());
        value.put("narrative_mode", self.narrativeMode());
        value.put("pov", self.pov());
        value.put("scene_id", self.sceneId().value());
        value.put("speaker_id", self.speakerId());
        value.put("stratum_kind", self.stratumKind().canonicalName());
        return CanonicalValues.freezeMap(value, "style_analysis_block");
    }
}
