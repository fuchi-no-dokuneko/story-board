package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.NarrativeBlock;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        blocks = List.copyOf(blocks);
        if (blocks.isEmpty() || blocks.size() > 1_000) {
            throw new IllegalArgumentException("Style extraction requires 1 to 1000 blocks");
        }
        Objects.requireNonNull(lexicon, "lexicon");
        Objects.requireNonNull(contract, "contract");
        if (!contract.vocabularyHash().equals(lexicon.vocabularyHash())) {
            throw new IllegalArgumentException(
                    "Style feature contract vocabulary does not match the masking lexicon"
            );
        }
        String sourceHash = sourceHash(blocks);
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

    public StyleDistanceReport compare(
            StyleFeatureSet target,
            StyleFeatureSet current
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(current, "current");
        String contractHash = target.contract().contractHash();
        if (!contractHash.equals(current.contract().contractHash())) {
            throw new IllegalArgumentException(
                    "Style feature sets with different contracts cannot be compared"
            );
        }
        boolean targetEmbedding = target.channels().stream().anyMatch(
                vector -> vector.channel() == StyleFeatureChannel.OPTIONAL_EMBEDDING
        );
        boolean currentEmbedding = current.channels().stream().anyMatch(
                vector -> vector.channel() == StyleFeatureChannel.OPTIONAL_EMBEDDING
        );
        if (targetEmbedding != currentEmbedding) {
            throw new IllegalArgumentException(
                    "Optional style embeddings must be present on both sides"
            );
        }

        List<StyleChannelDistance> distances = new ArrayList<>();
        for (StyleFeatureChannel channel : StyleFeatureChannel.values()) {
            if (channel == StyleFeatureChannel.OPTIONAL_EMBEDDING && !targetEmbedding) {
                continue;
            }
            StyleFeatureVector baseline = target.require(channel);
            StyleFeatureVector observed = current.require(channel);
            distances.add(StyleFeatureAnalyzerDistance.distance(
                    channel,
                    baseline,
                    observed,
                    target.contract().additiveSmoothingAlpha()
            ));
        }
        return new StyleDistanceReport(
                contractHash,
                target.featureSetHash(),
                current.featureSetHash(),
                distances,
                true
        );
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
