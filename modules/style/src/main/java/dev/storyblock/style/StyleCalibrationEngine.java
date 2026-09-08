package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class StyleCalibrationEngine {
    static final int SCALE = 12;
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
            StyleCalibrationEngineAdd.add(groups, candidate.window().requestedStratum(), candidate);
            if (candidate.window().requestedStratum().speakerSpecific()) {
                StyleCalibrationEngineAdd.add(groups, StyleStratum.dialogue(), candidate);
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
                        StyleCalibrationEngineAggregate.aggregate(remainder), windows.get(index).featureSet()
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

    record CalibrationGroup(
            StyleStratum stratum,
            List<StyleWindowFeatures> windows
    ) {
    }
}
