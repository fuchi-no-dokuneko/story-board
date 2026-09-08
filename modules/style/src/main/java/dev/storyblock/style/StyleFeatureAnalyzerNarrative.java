package dev.storyblock.style;

import dev.storyblock.domain.NarrativeBlock;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StyleFeatureAnalyzerNarrative {
    static StyleFeatureVector narrative(
            List<NarrativeBlock> blocks,
            StyleFeatureContract contract,
            String contractHash
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        String previousSpeaker = null;
        long speakerTurns = 0;
        for (NarrativeBlock block : blocks) {
            Map<String, Object> metadata = block.metadata().fields();
            Object speech = metadata.get("speech");
            boolean dialogue = StyleAnalysisBlock.isDialogue(block);
            boolean action = metadata.get("actions") instanceof List<?> values
                    && !values.isEmpty();
            StyleFeatureAnalyzerIncrement.increment(counts, dialogue ? "mode:dialogue"
                    : action ? "mode:action" : "mode:description");
            StyleFeatureAnalyzerIncrement.increment(counts, "narrative_mode:" + StyleFeatureAnalyzerScalar.scalar(metadata.get("narrative_mode")));
            StyleFeatureAnalyzerIncrement.increment(counts, "pov:" + StyleFeatureAnalyzerScalar.scalar(metadata.get("pov")));
            String speaker = StyleFeatureAnalyzerNestedString.nestedString(speech, "speaker_id");
            if (speaker != null) {
                StyleFeatureAnalyzerIncrement.increment(counts, "speaker:present");
                if (previousSpeaker != null && !previousSpeaker.equals(speaker)) {
                    speakerTurns++;
                }
                previousSpeaker = speaker;
            }
        }
        Map<String, BigDecimal> measurements = Map.of(
                "speaker_turn_ratio", StyleFeatureAnalyzerRatio.ratio(speakerTurns, Math.max(1, blocks.size() - 1L))
        );
        return StyleFeatureAnalyzerVector.vector(
                StyleFeatureChannel.NARRATIVE,
                contractHash,
                StyleFeatureAnalyzerDistribution.distribution(counts, contract.topK()),
                measurements
        );
    }
}
