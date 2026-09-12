package dev.storyblock.style;

import dev.storyblock.domain.NarrativeText;
import dev.storyblock.domain.UnicodeText;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static dev.storyblock.style.StyleFeatureAnalyzer.FUNCTION_WORDS;

final class StyleFeatureAnalyzerGrammar {
    static StyleFeatureVector grammar(
            List<? extends NarrativeText> blocks,
            StyleFeatureContract contract,
            String contractHash
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (NarrativeText block : blocks) {
            String text = Normalizer.normalize(
                    block.text(), Normalizer.Form.NFC
            ).toLowerCase(java.util.Locale.ROOT);
            for (String word : FUNCTION_WORDS) {
                int from = 0;
                while ((from = text.indexOf(word, from)) >= 0) {
                    StyleFeatureAnalyzerIncrement.increment(counts, "function:" + word);
                    from += word.length();
                }
            }
            List<String> shapes = UnicodeText.graphemes(text).stream()
                    .filter(value -> !value.isBlank())
                    .map(StyleFeatureAnalyzerShape::shape)
                    .toList();
            for (int index = 0; index + 2 <= shapes.size(); index++) {
                StyleFeatureAnalyzerIncrement.increment(counts, "pos2:" + shapes.get(index) + ">" + shapes.get(index + 1));
            }
        }
        return StyleFeatureAnalyzerVector.vector(
                StyleFeatureChannel.GRAMMAR,
                contractHash,
                StyleFeatureAnalyzerDistribution.distribution(counts, contract.topK()),
                Map.of()
        );
    }
}
