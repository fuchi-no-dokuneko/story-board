package dev.storyblock.style;

import dev.storyblock.domain.NarrativeText;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class StyleFeatureAnalyzerExtractAction {
    static StyleFeatureSet extract(StyleFeatureAnalyzer self, List<? extends NarrativeText> blocks, StyleMaskingLexicon lexicon, StyleFeatureContract contract, List<BigDecimal> contentReducedEmbedding)  {
        blocks = List.copyOf(blocks);
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("Style extraction requires at least one text block");
        }
        Objects.requireNonNull(lexicon, "lexicon");
        Objects.requireNonNull(contract, "contract");
        if (!contract.vocabularyHash().equals(lexicon.vocabularyHash())) {
            throw new IllegalArgumentException(
                    "Style feature contract vocabulary does not match the masking lexicon"
            );
        }
        String sourceHash = StyleFeatureAnalyzer.sourceHash(blocks);
        String contractHash = contract.contractHash();
        List<StyleFeatureVector> vectors = new ArrayList<>();
        vectors.add(StyleFeatureAnalyzerSurface.surface(blocks, lexicon, contract, contractHash));
        vectors.add(StyleFeatureAnalyzerGrammar.grammar(blocks, contract, contractHash));
        vectors.add(StyleFeatureAnalyzerRhythm.rhythm(blocks, contract, contractHash));
        vectors.add(StyleFeatureAnalyzerNarrative.narrative(blocks, contract, contractHash));
        vectors.add(StyleFeatureAnalyzerLexical.lexical(blocks, contract, contractHash));
        if (!contentReducedEmbedding.isEmpty()) {
            if (contentReducedEmbedding.size() < 2 || contentReducedEmbedding.size() > 4_096) {
                throw new IllegalArgumentException(
                        "Content-reduced style embedding must contain 2 to 4096 values"
                );
            }
            vectors.add(new StyleFeatureVector(
                    StyleFeatureChannel.OPTIONAL_EMBEDDING,
                    StyleFeatureChannel.OPTIONAL_EMBEDDING.featureVersion(),
                    contractHash,
                    Map.of(),
                    Map.of(),
                    contentReducedEmbedding
            ));
        }
        return new StyleFeatureSet(sourceHash, contract, vectors);
    }
}
