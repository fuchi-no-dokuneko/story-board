package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.TextAnalysis;
import dev.storyblock.domain.UnicodeText;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class StyleFeatureAnalyzer {
    private static final int SCALE = 12;
    private static final List<String> FUNCTION_WORDS = List.of(
            "的", "了", "著", "过", "過", "在", "是", "而", "但", "與", "与",
            "和", "或", "因為", "因为", "所以", "他", "她", "它", "我", "你",
            "we", "you", "he", "she", "it", "and", "or", "but", "because", "the"
    );
    private static final Set<String> SENTENCE_PUNCTUATION = Set.of(
            "。", "！", "？", "!", "?", ".", "…"
    );
    private static final Set<String> CLAUSE_PUNCTUATION = Set.of(
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
        vectors.add(surface(blocks, lexicon, contract, contractHash));
        vectors.add(grammar(blocks, contract, contractHash));
        vectors.add(rhythm(blocks, contract, contractHash));
        vectors.add(narrative(blocks, contract, contractHash));
        vectors.add(lexical(blocks, contract, contractHash));
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
            distances.add(distance(
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

    private static StyleFeatureVector surface(
            List<NarrativeBlock> blocks,
            StyleMaskingLexicon lexicon,
            StyleFeatureContract contract,
            String contractHash
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (NarrativeBlock block : blocks) {
            String masked = lexicon.mask(block.text());
            List<String> graphemes = UnicodeText.graphemes(masked).stream()
                    .filter(value -> !value.isBlank())
                    .toList();
            for (int size = 2; size <= 4; size++) {
                for (int index = 0; index + size <= graphemes.size(); index++) {
                    increment(counts, "char" + size + ":"
                            + String.join("", graphemes.subList(index, index + size)));
                }
            }
            for (String token : tokens(masked)) {
                increment(counts, "token:" + token);
            }
        }
        return vector(
                StyleFeatureChannel.SURFACE,
                contractHash,
                distribution(counts, contract.topK()),
                Map.of()
        );
    }

    private static StyleFeatureVector grammar(
            List<NarrativeBlock> blocks,
            StyleFeatureContract contract,
            String contractHash
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (NarrativeBlock block : blocks) {
            String text = Normalizer.normalize(
                    block.text(), Normalizer.Form.NFC
            ).toLowerCase(java.util.Locale.ROOT);
            for (String word : FUNCTION_WORDS) {
                int from = 0;
                while ((from = text.indexOf(word, from)) >= 0) {
                    increment(counts, "function:" + word);
                    from += word.length();
                }
            }
            List<String> shapes = UnicodeText.graphemes(text).stream()
                    .filter(value -> !value.isBlank())
                    .map(StyleFeatureAnalyzer::shape)
                    .toList();
            for (int index = 0; index + 2 <= shapes.size(); index++) {
                increment(counts, "pos2:" + shapes.get(index) + ">" + shapes.get(index + 1));
            }
        }
        return vector(
                StyleFeatureChannel.GRAMMAR,
                contractHash,
                distribution(counts, contract.topK()),
                Map.of()
        );
    }

    private static StyleFeatureVector rhythm(
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
                    increment(sentenceBuckets, bucket("sentence", length, 10));
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
        measurements.put("mean_sentence_graphemes", mean(sentenceLengths));
        measurements.put("mean_clause_graphemes", mean(clauseLengths));
        measurements.put("mean_paragraph_graphemes", mean(paragraphLengths));
        measurements.put("punctuation_ratio", ratio(punctuation, graphemes));
        return vector(
                StyleFeatureChannel.RHYTHM,
                contractHash,
                distribution(sentenceBuckets, contract.topK()),
                measurements
        );
    }

    private static StyleFeatureVector narrative(
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
            increment(counts, dialogue ? "mode:dialogue"
                    : action ? "mode:action" : "mode:description");
            increment(counts, "narrative_mode:" + scalar(metadata.get("narrative_mode")));
            increment(counts, "pov:" + scalar(metadata.get("pov")));
            String speaker = nestedString(speech, "speaker_id");
            if (speaker != null) {
                increment(counts, "speaker:present");
                if (previousSpeaker != null && !previousSpeaker.equals(speaker)) {
                    speakerTurns++;
                }
                previousSpeaker = speaker;
            }
        }
        Map<String, BigDecimal> measurements = Map.of(
                "speaker_turn_ratio", ratio(speakerTurns, Math.max(1, blocks.size() - 1L))
        );
        return vector(
                StyleFeatureChannel.NARRATIVE,
                contractHash,
                distribution(counts, contract.topK()),
                measurements
        );
    }

    private static StyleFeatureVector lexical(
            List<NarrativeBlock> blocks,
            StyleFeatureContract contract,
            String contractHash
    ) {
        List<String> tokens = new ArrayList<>();
        Map<String, Long> lengthBuckets = new LinkedHashMap<>();
        Map<String, Long> ngrams = new LinkedHashMap<>();
        for (NarrativeBlock block : blocks) {
            List<String> local = tokens(block.text());
            tokens.addAll(local);
            for (String token : local) {
                increment(lengthBuckets, bucket(
                        "token_length", UnicodeText.graphemeCount(token), 2
                ));
            }
            for (int index = 0; index + 3 <= local.size(); index++) {
                increment(ngrams, String.join("|", local.subList(index, index + 3)));
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
        measurements.put("type_token_ratio", ratio(
                new LinkedHashSet<>(tokens).size(), tokens.size()
        ));
        measurements.put("repeated_trigram_ratio", ratio(repeated, ngrams.size()));
        measurements.put("long_token_ratio", ratio(longTokens, tokens.size()));
        return vector(
                StyleFeatureChannel.LEXICAL,
                contractHash,
                distribution(lengthBuckets, contract.topK()),
                measurements
        );
    }

    private static StyleChannelDistance distance(
            StyleFeatureChannel channel,
            StyleFeatureVector target,
            StyleFeatureVector current,
            BigDecimal alpha
    ) {
        BigDecimal primary;
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        switch (channel.primaryMetric()) {
            case JENSEN_SHANNON_DISTANCE -> {
                primary = decimal(jsDistance(
                        target.distribution(), current.distribution(), alpha.doubleValue()
                ));
                if (channel == StyleFeatureChannel.SURFACE) {
                    diagnostics.put("kl_current_target", decimal(kl(
                            current.distribution(), target.distribution(), alpha.doubleValue()
                    )));
                    diagnostics.put("kl_target_current", decimal(kl(
                            target.distribution(), current.distribution(), alpha.doubleValue()
                    )));
                    diagnostics.put("token_kl_diagnostic_only", true);
                }
            }
            case WASSERSTEIN_DISTANCE -> {
                primary = decimal(wasserstein(
                        target.distribution(), current.distribution()
                ));
                diagnostics.put("jensen_shannon_secondary", decimal(jsDistance(
                        target.distribution(), current.distribution(), alpha.doubleValue()
                )));
            }
            case ROBUST_L1_INPUT -> {
                primary = decimal(l1(target, current));
                diagnostics.put("requires_profile_calibration", true);
            }
            case COSINE_DISTANCE -> {
                primary = decimal(cosineDistance(target.embedding(), current.embedding()));
                diagnostics.put("content_reduced_only", true);
                diagnostics.put("secondary_evidence_only", true);
            }
            default -> throw new IllegalStateException("Unsupported style distance metric");
        }
        diagnostics.put("top_contributors", topContributors(target, current, 10));
        return new StyleChannelDistance(
                channel,
                channel.featureVersion(),
                channel.primaryMetric(),
                primary,
                channel.required(),
                diagnostics
        );
    }

    private static List<Map<String, Object>> topContributors(
            StyleFeatureVector target,
            StyleFeatureVector current,
            int limit
    ) {
        Map<String, BigDecimal> targetValues = contributorValues(target);
        Map<String, BigDecimal> currentValues = contributorValues(current);
        Set<String> keys = new LinkedHashSet<>(targetValues.keySet());
        keys.addAll(currentValues.keySet());
        return keys.stream()
                .map(key -> Map.<String, Object>of(
                        "absolute_delta",
                        targetValues.getOrDefault(key, BigDecimal.ZERO)
                                .subtract(currentValues.getOrDefault(
                                        key, BigDecimal.ZERO
                                )).abs().stripTrailingZeros(),
                        "feature", key
                ))
                .sorted(Comparator
                        .<Map<String, Object>, BigDecimal>comparing(value ->
                                (BigDecimal) value.get("absolute_delta")
                        ).reversed()
                        .thenComparing(value -> (String) value.get("feature")))
                .limit(limit)
                .toList();
    }

    private static Map<String, BigDecimal> contributorValues(
            StyleFeatureVector vector
    ) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        vector.distribution().forEach((key, value) ->
                values.put("distribution:" + key, value)
        );
        vector.measurements().forEach((key, value) ->
                values.put("measurement:" + key, value)
        );
        for (int index = 0; index < vector.embedding().size(); index++) {
            values.put("embedding:" + index, vector.embedding().get(index));
        }
        return Map.copyOf(values);
    }

    private static StyleFeatureVector vector(
            StyleFeatureChannel channel,
            String contractHash,
            Map<String, BigDecimal> distribution,
            Map<String, BigDecimal> measurements
    ) {
        return new StyleFeatureVector(
                channel,
                channel.featureVersion(),
                contractHash,
                distribution,
                measurements,
                List.of()
        );
    }

    private static Map<String, BigDecimal> distribution(
            Map<String, Long> rawCounts,
            int topK
    ) {
        if (rawCounts.isEmpty()) {
            return Map.of("OTHER", BigDecimal.ONE);
        }
        List<Map.Entry<String, Long>> ordered = rawCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        Map<String, Long> retained = new LinkedHashMap<>();
        long other = 0;
        for (int index = 0; index < ordered.size(); index++) {
            Map.Entry<String, Long> entry = ordered.get(index);
            if (index < topK) {
                retained.put(entry.getKey(), entry.getValue());
            } else {
                other += entry.getValue();
            }
        }
        if (other > 0) {
            retained.put("OTHER", other);
        }
        long total = retained.values().stream().mapToLong(Long::longValue).sum();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        retained.forEach((key, count) -> result.put(
                key,
                BigDecimal.valueOf(count).divide(
                        BigDecimal.valueOf(total), SCALE, RoundingMode.HALF_EVEN
                ).stripTrailingZeros()
        ));
        return Map.copyOf(result);
    }

    private static String sourceHash(List<NarrativeBlock> blocks) {
        return CanonicalJson.hash(blocks.stream().map(block -> Map.of(
                "block_id", block.id().value(),
                "block_version_id", block.versionId().value(),
                "extensions", block.extensions(),
                "meta", block.metadata().fields(),
                "text", block.text()
        )).toList());
    }

    private static List<String> tokens(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (String grapheme : UnicodeText.graphemes(text)) {
            int codePoint = grapheme.codePointAt(0);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                flush(word, result);
                result.add(grapheme);
            } else if (Character.isLetterOrDigit(codePoint)) {
                word.append(grapheme.toLowerCase(java.util.Locale.ROOT));
            } else {
                flush(word, result);
            }
        }
        flush(word, result);
        return List.copyOf(result);
    }

    private static void flush(StringBuilder word, List<String> result) {
        if (!word.isEmpty()) {
            result.add(word.toString());
            word.setLength(0);
        }
    }

    private static String shape(String grapheme) {
        int codePoint = grapheme.codePointAt(0);
        if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
            return "HAN";
        }
        if (Character.isLetter(codePoint)) {
            return "LETTER";
        }
        if (Character.isDigit(codePoint)) {
            return "NUMBER";
        }
        if (SENTENCE_PUNCTUATION.contains(grapheme)) {
            return "SENTENCE_END";
        }
        if (CLAUSE_PUNCTUATION.contains(grapheme)) {
            return "CLAUSE_END";
        }
        return "OTHER";
    }

    private static String scalar(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        if (value instanceof Map<?, ?> map) {
            Object nested = map.get("value");
            if (nested instanceof String text && !text.isBlank()) {
                return text;
            }
            Object mode = map.get("mode");
            if (mode instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return "unknown";
    }

    private static String nestedString(Object value, String field) {
        if (value instanceof Map<?, ?> map && map.get(field) instanceof String text
                && !text.isBlank()) {
            return text;
        }
        return null;
    }

    private static String bucket(String prefix, int value, int width) {
        int lower = Math.max(0, value / width * width);
        return prefix + ":" + lower;
    }

    private static BigDecimal mean(List<Integer> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long sum = values.stream().mapToLong(Integer::longValue).sum();
        return BigDecimal.valueOf(sum).divide(
                BigDecimal.valueOf(values.size()), SCALE, RoundingMode.HALF_EVEN
        ).stripTrailingZeros();
    }

    private static BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).divide(
                BigDecimal.valueOf(denominator), SCALE, RoundingMode.HALF_EVEN
        ).stripTrailingZeros();
    }

    private static void increment(Map<String, Long> counts, String key) {
        counts.merge(key, 1L, Long::sum);
    }

    private static double jsDistance(
            Map<String, BigDecimal> left,
            Map<String, BigDecimal> right,
            double alpha
    ) {
        Probabilities values = probabilities(left, right, alpha);
        double divergence = 0;
        for (int index = 0; index < values.left().length; index++) {
            double middle = (values.left()[index] + values.right()[index]) / 2.0;
            divergence += 0.5 * values.left()[index]
                    * StrictMath.log(values.left()[index] / middle);
            divergence += 0.5 * values.right()[index]
                    * StrictMath.log(values.right()[index] / middle);
        }
        return StrictMath.sqrt(Math.max(0, divergence));
    }

    private static double kl(
            Map<String, BigDecimal> left,
            Map<String, BigDecimal> right,
            double alpha
    ) {
        Probabilities values = probabilities(left, right, alpha);
        double result = 0;
        for (int index = 0; index < values.left().length; index++) {
            result += values.left()[index]
                    * StrictMath.log(values.left()[index] / values.right()[index]);
        }
        return Math.max(0, result);
    }

    private static Probabilities probabilities(
            Map<String, BigDecimal> left,
            Map<String, BigDecimal> right,
            double alpha
    ) {
        Set<String> keys = new java.util.TreeSet<>();
        keys.addAll(left.keySet());
        keys.addAll(right.keySet());
        if (keys.isEmpty()) {
            keys.add("OTHER");
        }
        double leftTotal = left.values().stream().mapToDouble(BigDecimal::doubleValue).sum()
                + alpha * keys.size();
        double rightTotal = right.values().stream().mapToDouble(BigDecimal::doubleValue).sum()
                + alpha * keys.size();
        double[] normalizedLeft = new double[keys.size()];
        double[] normalizedRight = new double[keys.size()];
        int index = 0;
        for (String key : keys) {
            normalizedLeft[index] = (left.getOrDefault(key, BigDecimal.ZERO).doubleValue()
                    + alpha) / leftTotal;
            normalizedRight[index] = (right.getOrDefault(key, BigDecimal.ZERO).doubleValue()
                    + alpha) / rightTotal;
            index++;
        }
        return new Probabilities(normalizedLeft, normalizedRight);
    }

    private static double wasserstein(
            Map<String, BigDecimal> left,
            Map<String, BigDecimal> right
    ) {
        Set<String> keys = new java.util.TreeSet<>(Comparator.comparingInt(
                StyleFeatureAnalyzer::bucketStart
        ).thenComparing(value -> value));
        keys.addAll(left.keySet());
        keys.addAll(right.keySet());
        double cumulative = 0;
        double result = 0;
        for (String key : keys) {
            cumulative += left.getOrDefault(key, BigDecimal.ZERO).doubleValue()
                    - right.getOrDefault(key, BigDecimal.ZERO).doubleValue();
            result += StrictMath.abs(cumulative);
        }
        return keys.isEmpty() ? 0 : result / keys.size();
    }

    private static int bucketStart(String key) {
        int colon = key.lastIndexOf(':');
        if (colon < 0) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(key.substring(colon + 1));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static double l1(StyleFeatureVector left, StyleFeatureVector right) {
        Map<String, BigDecimal> leftValues = new LinkedHashMap<>(left.distribution());
        left.measurements().forEach((key, value) -> leftValues.put("measure:" + key, value));
        Map<String, BigDecimal> rightValues = new LinkedHashMap<>(right.distribution());
        right.measurements().forEach((key, value) -> rightValues.put("measure:" + key, value));
        Set<String> keys = new LinkedHashSet<>(leftValues.keySet());
        keys.addAll(rightValues.keySet());
        return keys.stream().mapToDouble(key -> StrictMath.abs(
                leftValues.getOrDefault(key, BigDecimal.ZERO).doubleValue()
                        - rightValues.getOrDefault(key, BigDecimal.ZERO).doubleValue()
        )).average().orElse(0);
    }

    private static double cosineDistance(List<BigDecimal> left, List<BigDecimal> right) {
        if (left.size() != right.size()) {
            throw new IllegalArgumentException("Style embedding dimensions do not match");
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index).doubleValue();
            double rightValue = right.get(index).doubleValue();
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            throw new IllegalArgumentException("Style embedding cannot be a zero vector");
        }
        double similarity = dot / (StrictMath.sqrt(leftNorm) * StrictMath.sqrt(rightNorm));
        return Math.max(0, Math.min(2, 1 - similarity));
    }

    private static BigDecimal decimal(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Style distance is not finite");
        }
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_EVEN)
                .stripTrailingZeros();
    }

    private record Probabilities(double[] left, double[] right) {
    }
}
