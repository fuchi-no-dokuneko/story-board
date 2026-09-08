package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.util.Map;

final class StyleAnalysisBlockFromCanonicalFactory {
    static StyleAnalysisBlock fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleAnalysisBlock.FIELDS, "style_analysis_block");
        Map<String, Object> block = StyleCanonical.object(
                value.get("block"), "style_analysis_block.block"
        );
        StyleCanonical.requireKeys(block, StyleAnalysisBlock.BLOCK_FIELDS, "style_analysis_block.block");
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
}
