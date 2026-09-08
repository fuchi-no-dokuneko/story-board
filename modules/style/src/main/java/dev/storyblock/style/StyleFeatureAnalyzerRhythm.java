package dev.storyblock.style;

import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.TextAnalysis;
import dev.storyblock.domain.UnicodeText;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static dev.storyblock.style.StyleFeatureAnalyzer.SENTENCE_PUNCTUATION;
import static dev.storyblock.style.StyleFeatureAnalyzer.CLAUSE_PUNCTUATION;

final class StyleFeatureAnalyzerRhythm {
    static StyleFeatureVector rhythm(
            List<NarrativeBlock> blocks,
            StyleFeatureContract contract,
            String contractHash
    ) {
        Map<String, Long> sentenceBuckets = new LinkedHashMap<>();
        List<Integer> sentenceLengths = new ArrayList<>();
        List<Integer> clauseLengths = new ArrayList<>();
        List<Integer> paragraphLengths = new ArrayList<>();
        long punctuation = 0;
        long graphemes = 0;
        for (NarrativeBlock block : blocks) {
            TextAnalysis analysis = UnicodeText.analyze(block.text());
            paragraphLengths.add(analysis.graphemeCount());
            graphemes += analysis.graphemeCount();
            List<Integer> boundaries = new ArrayList<>(analysis.safeSplitAnchors());
            boundaries.add(analysis.graphemeCount());
            int previous = 0;
            for (int boundary : boundaries) {
                int length = boundary - previous;
                if (length > 0) {
                    sentenceLengths.add(length);
                    StyleFeatureAnalyzerIncrement.increment(sentenceBuckets, StyleFeatureAnalyzerBucket.bucket("sentence", length, 10));
                }
                previous = boundary;
            }
            int clause = 0;
            for (String unit : UnicodeText.graphemes(block.text())) {
                if (SENTENCE_PUNCTUATION.contains(unit)
                        || CLAUSE_PUNCTUATION.contains(unit)) {
                    punctuation++;
                    if (clause > 0) {
                        clauseLengths.add(clause);
                        clause = 0;
                    }
                } else if (!unit.isBlank()) {
                    clause++;
                }
            }
            if (clause > 0) {
                clauseLengths.add(clause);
            }
        }
        Map<String, BigDecimal> measurements = new LinkedHashMap<>();
        measurements.put("mean_sentence_graphemes", StyleFeatureAnalyzerMean.mean(sentenceLengths));
        measurements.put("mean_clause_graphemes", StyleFeatureAnalyzerMean.mean(clauseLengths));
        measurements.put("mean_paragraph_graphemes", StyleFeatureAnalyzerMean.mean(paragraphLengths));
        measurements.put("punctuation_ratio", StyleFeatureAnalyzerRatio.ratio(punctuation, graphemes));
        return StyleFeatureAnalyzerVector.vector(
                StyleFeatureChannel.RHYTHM,
                contractHash,
                StyleFeatureAnalyzerDistribution.distribution(sentenceBuckets, contract.topK()),
                measurements
        );
    }
}
