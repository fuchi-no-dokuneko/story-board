package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class StyleCalibrationEngine {
    private static final int SCALE = 12;
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    private final StyleFeatureAnalyzer analyzer;

    public StyleCalibrationEngine() {
        this(new StyleFeatureAnalyzer());
    }

    StyleCalibrationEngine(StyleFeatureAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
    }

    public StyleCalibrationProfile calibrate(
            String targetCorpusHash,
            StyleWindowConfiguration configuration,
            List<StyleWindowFeatures> windows
    ) {
        if (targetCorpusHash == null || !HASH.matcher(targetCorpusHash).matches()) {
            throw new IllegalArgumentException(
                    "Style calibration target corpus hash is invalid"
            );
        }
        Objects.requireNonNull(configuration, "configuration");
        List<StyleWindowFeatures> eligible = List.copyOf(windows).stream()
                .filter(candidate -> candidate.window().primaryDecisionEligible())
                .sorted(Comparator.comparing(candidate -> candidate.window().windowId()))
                .toList();
        if (eligible.isEmpty()) {
            throw new IllegalArgumentException(
                    "Style calibration requires at least one full operational window"
            );
        }
        if (eligible.stream().map(candidate -> candidate.window().windowId())
                .distinct().count() != eligible.size()) {
            throw new IllegalArgumentException(
                    "Style calibration windows must have unique identities"
            );
        }
        String contractHash = eligible.getFirst().featureSet().contract().contractHash();
        for (StyleWindowFeatures candidate : eligible) {
            if (!contractHash.equals(
                    candidate.featureSet().contract().contractHash()
            )) {
                throw new IllegalArgumentException(
                        "Style calibration windows must use one feature contract"
                );
            }
        }

        Map<String, CalibrationGroup> groups = new TreeMap<>();
        for (StyleWindowFeatures candidate : eligible) {
            add(groups, candidate.window().requestedStratum(), candidate);
            if (candidate.window().requestedStratum().speakerSpecific()) {
                add(groups, StyleStratum.dialogue(), candidate);
            }
        }
        List<StyleStratumCalibration> strata = groups.values().stream()
                .map(this::calibrate)
                .toList();
        return new StyleCalibrationProfile(
                StyleModule.CALIBRATION_SCHEMA_VERSION,
                targetCorpusHash,
                contractHash,
                configuration.configurationHash(),
                strata
        );
    }

    private StyleStratumCalibration calibrate(CalibrationGroup group) {
        List<StyleWindowFeatures> windows = group.windows();
        EnumMap<StyleFeatureChannel, List<BigDecimal>> distances = new EnumMap<>(
                StyleFeatureChannel.class
        );
        windows.getFirst().featureSet().channels().forEach(vector ->
                distances.put(vector.channel(), new ArrayList<>())
        );
        if (windows.size() > 1) {
            for (int index = 0; index < windows.size(); index++) {
                List<StyleFeatureSet> remainder = new ArrayList<>();
                for (int candidate = 0; candidate < windows.size(); candidate++) {
                    if (candidate != index) {
                        remainder.add(windows.get(candidate).featureSet());
                    }
                }
                StyleDistanceReport report = analyzer.compare(
                        aggregate(remainder), windows.get(index).featureSet()
                );
                report.channels().forEach(distance ->
                        distances.get(distance.channel()).add(distance.primaryDistance())
                );
            }
        }
        List<StyleChannelCalibration> channels = new ArrayList<>();
        for (StyleFeatureChannel channel : StyleFeatureChannel.values()) {
            if (distances.containsKey(channel)) {
                channels.add(StyleChannelCalibration.fromDistances(
                        channel, distances.get(channel)
                ));
            }
        }
        return new StyleStratumCalibration(
                group.stratum(), windows.size(), channels
        );
    }

    private static StyleFeatureSet aggregate(List<StyleFeatureSet> featureSets) {
        featureSets = List.copyOf(featureSets);
        if (featureSets.isEmpty()) {
            throw new IllegalArgumentException("Cannot aggregate zero style feature sets");
        }
        StyleFeatureContract contract = featureSets.getFirst().contract();
        String contractHash = contract.contractHash();
        boolean hasEmbedding = hasChannel(
                featureSets.getFirst(), StyleFeatureChannel.OPTIONAL_EMBEDDING
        );
        for (StyleFeatureSet featureSet : featureSets) {
            if (!contractHash.equals(featureSet.contract().contractHash())
                    || hasEmbedding != hasChannel(
                            featureSet, StyleFeatureChannel.OPTIONAL_EMBEDDING
                    )) {
                throw new IllegalArgumentException(
                        "Aggregated style features must use one complete contract"
                );
            }
        }

        List<StyleFeatureVector> vectors = new ArrayList<>();
        for (StyleFeatureChannel channel : StyleFeatureChannel.values()) {
            if (channel == StyleFeatureChannel.OPTIONAL_EMBEDDING && !hasEmbedding) {
                continue;
            }
            List<StyleFeatureVector> source = featureSets.stream()
                    .map(featureSet -> featureSet.require(channel))
                    .toList();
            vectors.add(new StyleFeatureVector(
                    channel,
                    channel.featureVersion(),
                    contractHash,
                    channel == StyleFeatureChannel.OPTIONAL_EMBEDDING
                            ? Map.of()
                            : averagedDistribution(source, contract.topK()),
                    channel == StyleFeatureChannel.OPTIONAL_EMBEDDING
                            ? Map.of()
                            : averagedMeasurements(source),
                    channel == StyleFeatureChannel.OPTIONAL_EMBEDDING
                            ? averagedEmbedding(source)
                            : List.of()
            ));
        }
        List<String> componentHashes = featureSets.stream()
                .map(StyleFeatureSet::featureSetHash)
                .sorted()
                .toList();
        return new StyleFeatureSet(
                CanonicalJson.hash(componentHashes), contract, vectors
        );
    }

    private static Map<String, BigDecimal> averagedDistribution(
            List<StyleFeatureVector> vectors,
            int topK
    ) {
        Map<String, BigDecimal> average = averagedMap(
                vectors.stream().map(StyleFeatureVector::distribution).toList(),
                vectors.size()
        );
        BigDecimal other = average.getOrDefault("OTHER", BigDecimal.ZERO);
        List<Map.Entry<String, BigDecimal>> ordered = average.entrySet().stream()
                .filter(entry -> !"OTHER".equals(entry.getKey()))
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int index = 0; index < ordered.size(); index++) {
            Map.Entry<String, BigDecimal> entry = ordered.get(index);
            if (index < topK) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                other = other.add(entry.getValue());
            }
        }
        if (other.signum() > 0 || result.isEmpty()) {
            result.put("OTHER", other.signum() == 0 ? BigDecimal.ONE : other);
        }
        return Map.copyOf(result);
    }

    private static Map<String, BigDecimal> averagedMeasurements(
            List<StyleFeatureVector> vectors
    ) {
        return averagedMap(
                vectors.stream().map(StyleFeatureVector::measurements).toList(),
                vectors.size()
        );
    }

    private static Map<String, BigDecimal> averagedMap(
            List<Map<String, BigDecimal>> values,
            int count
    ) {
        Set<String> keys = new LinkedHashSet<>();
        values.forEach(value -> keys.addAll(value.keySet()));
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String key : keys) {
            BigDecimal sum = values.stream()
                    .map(value -> value.getOrDefault(key, BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(key, sum.divide(
                    BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_EVEN
            ).stripTrailingZeros());
        }
        return Map.copyOf(result);
    }

    private static List<BigDecimal> averagedEmbedding(
            List<StyleFeatureVector> vectors
    ) {
        int dimensions = vectors.getFirst().embedding().size();
        if (vectors.stream().anyMatch(vector ->
                vector.embedding().size() != dimensions
        )) {
            throw new IllegalArgumentException(
                    "Style calibration embedding dimensions must match"
            );
        }
        List<BigDecimal> result = new ArrayList<>();
        for (int dimension = 0; dimension < dimensions; dimension++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (StyleFeatureVector vector : vectors) {
                sum = sum.add(vector.embedding().get(dimension));
            }
            result.add(sum.divide(
                    BigDecimal.valueOf(vectors.size()),
                    SCALE,
                    RoundingMode.HALF_EVEN
            ).stripTrailingZeros());
        }
        return List.copyOf(result);
    }

    private static boolean hasChannel(
            StyleFeatureSet featureSet,
            StyleFeatureChannel channel
    ) {
        return featureSet.channels().stream().anyMatch(vector ->
                vector.channel() == channel
        );
    }

    private static void add(
            Map<String, CalibrationGroup> groups,
            StyleStratum stratum,
            StyleWindowFeatures window
    ) {
        groups.computeIfAbsent(
                stratum.canonicalKey(), ignored -> new CalibrationGroup(
                        stratum, new ArrayList<>()
                )
        ).windows().add(window);
    }

    private record CalibrationGroup(
            StyleStratum stratum,
            List<StyleWindowFeatures> windows
    ) {
    }
}
