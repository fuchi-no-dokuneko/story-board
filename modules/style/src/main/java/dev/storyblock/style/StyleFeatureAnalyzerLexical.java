package dev.storyblock.style;

import dev.storyblock.domain.NarrativeText;
import dev.storyblock.domain.UnicodeText;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class StyleFeatureAnalyzerLexical {
    static StyleFeatureVector lexical(
            List<? extends NarrativeText> blocks,
            StyleFeatureContract contract,
            String contractHash
    ) {
        List<String> tokens = new ArrayList<>();
        Map<String, Long> lengthBuckets = new LinkedHashMap<>();
        Map<String, Long> ngrams = new LinkedHashMap<>();
        for (NarrativeText block : blocks) {
            List<String> local = StyleFeatureAnalyzerTokens.tokens(block.text());
            tokens.addAll(local);
            for (String token : local) {
                StyleFeatureAnalyzerIncrement.increment(lengthBuckets, StyleFeatureAnalyzerBucket.bucket(
                        "token_length", UnicodeText.graphemeCount(token), 2
                ));
            }
            for (int index = 0; index + 3 <= local.size(); index++) {
                StyleFeatureAnalyzerIncrement.increment(ngrams, String.join("|", local.subList(index, index + 3)));
            }
        }
        long repeated = ngrams.values().stream()
                .filter(count -> count > 1)
                .mapToLong(count -> count - 1)
                .sum();
        long longTokens = tokens.stream()
                .filter(token -> UnicodeText.graphemeCount(token) >= 4)
                .count();
        Map<String, BigDecimal> measurements = new LinkedHashMap<>();
        measurements.put("type_token_ratio", StyleFeatureAnalyzerRatio.ratio(
                new LinkedHashSet<>(tokens).size(), tokens.size()
        ));
        measurements.put("repeated_trigram_ratio", StyleFeatureAnalyzerRatio.ratio(repeated, ngrams.size()));
        measurements.put("long_token_ratio", StyleFeatureAnalyzerRatio.ratio(longTokens, tokens.size()));
        return StyleFeatureAnalyzerVector.vector(
                StyleFeatureChannel.LEXICAL,
                contractHash,
                StyleFeatureAnalyzerDistribution.distribution(lengthBuckets, contract.topK()),
                measurements
        );
    }
}
