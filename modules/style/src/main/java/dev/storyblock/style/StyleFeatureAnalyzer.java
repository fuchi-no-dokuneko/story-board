package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.NarrativeBlock;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StyleFeatureAnalyzer {
    static final int SCALE = 12;
    static final List<String> FUNCTION_WORDS = List.of(
            "的", "了", "著", "过", "過", "在", "是", "而", "但", "與", "与",
            "和", "或", "因為", "因为", "所以", "他", "她", "它", "我", "你",
            "we", "you", "he", "she", "it", "and", "or", "but", "because", "the"
    );
    static final Set<String> SENTENCE_PUNCTUATION = Set.of(
            "。", "！", "？", "!", "?", ".", "…"
    );
    static final Set<String> CLAUSE_PUNCTUATION = Set.of(
            "，", ",", "；", ";", "：", ":", "、"
    );

    public StyleFeatureSet extract(
            List<NarrativeBlock> blocks,
            StyleMaskingLexicon lexicon,
            StyleFeatureContract contract
    ) {
        return extract(blocks, lexicon, contract, List.of());
    }

    public StyleFeatureSet extract(
            List<NarrativeBlock> blocks,
            StyleMaskingLexicon lexicon,
            StyleFeatureContract contract,
            List<BigDecimal> contentReducedEmbedding
    ) {
        return StyleFeatureAnalyzerExtractAction.extract(this, blocks, lexicon, contract, contentReducedEmbedding);
    }

    public StyleDistanceReport compare(
            StyleFeatureSet target,
            StyleFeatureSet current
    ) {
        return StyleFeatureAnalyzerCompareAction.compare(this, target, current);
    }

    public static String sourceHash(List<NarrativeBlock> blocks) {
        blocks = List.copyOf(blocks);
        if (blocks.isEmpty() || blocks.size() > 1_000) {
            throw new IllegalArgumentException(
                    "Style source hash requires 1 to 1000 blocks"
            );
        }
        return CanonicalJson.hash(blocks.stream().map(block -> Map.of(
                "block_id", block.id().value(),
                "block_version_id", block.versionId().value(),
                "extensions", block.extensions(),
                "meta", block.metadata().fields(),
                "text", block.text()
        )).toList());
    }

    record Probabilities(double[] left, double[] right) {
    }
}
