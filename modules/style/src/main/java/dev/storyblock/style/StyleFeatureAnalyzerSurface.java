package dev.storyblock.style;

import dev.storyblock.domain.NarrativeText;
import dev.storyblock.domain.UnicodeText;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StyleFeatureAnalyzerSurface {
    static StyleFeatureVector surface(
            List<? extends NarrativeText> blocks,
            StyleMaskingLexicon lexicon,
            StyleFeatureContract contract,
            String contractHash
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (NarrativeText block : blocks) {
            String masked = lexicon.mask(block.text());
            List<String> graphemes = UnicodeText.graphemes(masked).stream()
                    .filter(value -> !value.isBlank())
                    .toList();
            for (int size = 2; size <= 4; size++) {
                for (int index = 0; index + size <= graphemes.size(); index++) {
                    StyleFeatureAnalyzerIncrement.increment(counts, "char" + size + ":"
                            + String.join("", graphemes.subList(index, index + size)));
                }
            }
            for (String token : StyleFeatureAnalyzerTokens.tokens(masked)) {
                StyleFeatureAnalyzerIncrement.increment(counts, "token:" + token);
            }
        }
        return StyleFeatureAnalyzerVector.vector(
                StyleFeatureChannel.SURFACE,
                contractHash,
                StyleFeatureAnalyzerDistribution.distribution(counts, contract.topK()),
                Map.of()
        );
    }
}
