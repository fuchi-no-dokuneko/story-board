package dev.storyblock.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StyleAnomalyPolicyTest {
    private static final String CONTRACT_HASH = CanonicalJson.hash("feature-contract");

    private final StyleWindowScorer scorer = new StyleWindowScorer();
    private final StyleAnomalyPolicy policy = new StyleAnomalyPolicy();

    @Test
    void twoSustainedQ99ChannelsCanCreateRewriteCandidate() {
        StyleCalibrationProfile profile = calibratedProfile(30);
        StyleWindowScore operational = score(
                window(StyleWindowKind.OPERATIONAL, null), highReport(), profile
        );
        List<StyleWindowScore> sustained = List.of(
                score(window(StyleWindowKind.NON_OVERLAP, null), highReport(), profile),
                score(window(StyleWindowKind.NON_OVERLAP, null), highReport(), profile)
        );

        StyleAnomalyDecision decision = policy.evaluate(
                operational, sustained, List.of()
        );

        assertEquals(StyleDecisionState.REWRITE_CANDIDATE, decision.state());
        assertTrue(decision.canTriggerRewrite());
        assertEquals(2, decision.sustainingWindowIds().size());
        assertEquals(
                List.of(StyleFeatureChannel.SURFACE, StyleFeatureChannel.GRAMMAR),
                decision.independentQ99Channels()
        );
        assertEquals(
                decision,
                StyleAnomalyDecision.fromCanonical(decision.canonicalValue())
        );
    }

    @Test
    void topicOnlyLowConfidenceAndMicroOnlySignalsNeverTriggerRewrite() {
        StyleCalibrationProfile calibrated = calibratedProfile(30);
        StyleWindow operationalWindow = window(StyleWindowKind.OPERATIONAL, null);
        StyleAnomalyDecision topic = policy.evaluate(
                score(operationalWindow, topicOnlyReport(), calibrated),
                List.of(),
                List.of()
        );
        assertEquals(StyleDecisionState.TOPIC_SHIFT_ONLY, topic.state());
        assertFalse(topic.canTriggerRewrite());

        StyleAnomalyDecision low = policy.evaluate(
                score(operationalWindow, highReport(), calibratedProfile(29)),
                List.of(),
                List.of()
        );
        assertEquals(StyleDecisionState.LOW_CONFIDENCE, low.state());
        assertFalse(low.canTriggerRewrite());

        StyleWindowScore micro = score(
                window(StyleWindowKind.MICRO, null), highReport(), calibrated
        );
        StyleAnomalyDecision microOnly = policy.evaluate(
                score(operationalWindow, normalReport(), calibrated),
                List.of(),
                List.of(micro)
        );
        assertEquals(StyleDecisionState.NORMAL, microOnly.state());
        assertFalse(microOnly.canTriggerRewrite());
        assertEquals(List.of(micro.window().windowId()),
                microOnly.localizedMicroWindowIds());
    }

    @Test
    void intentionalShiftDowngradesARewriteCandidate() {
        StyleCalibrationProfile profile = calibratedProfile(30);
        StyleWindowScore operational = score(
                window(StyleWindowKind.OPERATIONAL, "Dream sequence"),
                highReport(),
                profile
        );
        List<StyleWindowScore> sustained = List.of(
                score(window(
                        StyleWindowKind.NON_OVERLAP, "Dream sequence"
                ), highReport(), profile),
                score(window(
                        StyleWindowKind.NON_OVERLAP, "Dream sequence"
                ), highReport(), profile)
        );

        StyleAnomalyDecision decision = policy.evaluate(
                operational, sustained, List.of()
        );

        assertEquals(StyleDecisionState.WARNING, decision.state());
        assertEquals(StyleDecisionReason.INTENTIONAL_STYLE_SHIFT, decision.reason());
        assertTrue(decision.intentionalShiftAdjusted());
        assertFalse(decision.canTriggerRewrite());
    }

    @Test
    void overlappingNonOverlapWindowsCannotSustainARewrite() {
        StyleCalibrationProfile profile = calibratedProfile(30);
        StyleWindowScore operational = score(
                window(StyleWindowKind.OPERATIONAL, null), highReport(), profile
        );
        Ids.BlockId shared = Ids.BlockId.create();
        StyleWindowScore first = score(window(
                StyleWindowKind.NON_OVERLAP,
                List.of(Ids.BlockId.create(), shared),
                null
        ), highReport(), profile);
        StyleWindowScore second = score(window(
                StyleWindowKind.NON_OVERLAP,
                List.of(shared, Ids.BlockId.create()),
                null
        ), highReport(), profile);

        assertThrows(IllegalArgumentException.class, () -> policy.evaluate(
                operational, List.of(first, second), List.of()
        ));
    }

    @Test
    void labeledCalibrationEvaluationReportsConfusionChannelsAndPercentiles()
            throws Exception {
        StyleCalibrationProfile profile = calibratedProfile(30);
        List<EvaluationCase> corpus = List.of(
                new EvaluationCase("train-normal", "train", false,
                        normalReport(), List.of(), null),
                new EvaluationCase("train-topic", "train", false,
                        topicOnlyReport(), List.of(), null),
                new EvaluationCase("calibration-drift", "calibration", true,
                        highReport(), List.of(highReport(), highReport()), null),
                new EvaluationCase("calibration-intentional", "calibration", false,
                        highReport(), List.of(highReport(), highReport()),
                        "Intentional dream sequence")
        );
        int truePositive = 0;
        int trueNegative = 0;
        int falsePositive = 0;
        int falseNegative = 0;
        Map<String, Integer> channelContributions = new LinkedHashMap<>();
        List<Map<String, Object>> cases = new ArrayList<>();

        for (EvaluationCase value : corpus) {
            StyleWindowScore operational = score(
                    window(StyleWindowKind.OPERATIONAL, value.shiftReason()),
                    value.operational(),
                    profile
            );
            List<StyleWindowScore> sustained = value.sustained().stream()
                    .map(report -> score(window(
                            StyleWindowKind.NON_OVERLAP, value.shiftReason()
                    ), report, profile))
                    .toList();
            StyleAnomalyDecision decision = policy.evaluate(
                    operational, sustained, List.of()
            );
            boolean actual = decision.canTriggerRewrite();
            if (value.expected() && actual) {
                truePositive++;
            } else if (!value.expected() && !actual) {
                trueNegative++;
            } else if (actual) {
                falsePositive++;
            } else {
                falseNegative++;
            }
            decision.independentQ99Channels().forEach(channel ->
                    channelContributions.merge(channel.canonicalName(), 1, Integer::sum)
            );
            cases.add(Map.of(
                    "actual_rewrite_candidate", actual,
                    "expected_rewrite_candidate", value.expected(),
                    "id", value.id(),
                    "label", value.shiftReason() == null
                            ? "unwanted_or_normal" : "intentional_shift",
                    "split", value.split(),
                    "state", decision.state().canonicalName()
            ));
        }

        StyleWindowScore before = score(
                window(StyleWindowKind.OPERATIONAL, null), highReport(), profile
        );
        StyleWindowScore after = score(
                window(StyleWindowKind.OPERATIONAL, null), normalReport(), profile
        );
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("cases", cases);
        report.put("channel_contributions", channelContributions);
        report.put("confusion", Map.of(
                "false_negative", falseNegative,
                "false_positive", falsePositive,
                "true_negative", trueNegative,
                "true_positive", truePositive
        ));
        report.put("corpus_split", Map.of("calibration", 2, "train", 2));
        report.put("rewrite_percentiles", Map.of(
                "after", percentiles(after),
                "before", percentiles(before)
        ));
        report.put("schema_version", "adr-317-style-evaluation-1");
        Path output = Path.of("target/evaluations/style-policy.json");
        Files.createDirectories(output.getParent());
        Files.write(output, CanonicalJson.bytes(report));

        assertEquals(1, truePositive);
        assertEquals(3, trueNegative);
        assertEquals(0, falsePositive);
        assertEquals(0, falseNegative);
        assertEquals(0, new BigDecimal("100").compareTo(
                before.channels().getFirst().percentile()
        ));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                after.channels().getFirst().percentile()
        ));
    }

    private static Map<String, BigDecimal> percentiles(StyleWindowScore score) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        score.channels().forEach(channel -> result.put(
                channel.distance().channel().canonicalName(), channel.percentile()
        ));
        return result;
    }

    private record EvaluationCase(
            String id,
            String split,
            boolean expected,
            StyleDistanceReport operational,
            List<StyleDistanceReport> sustained,
            String shiftReason
    ) {
    }

    private static StyleCalibrationProfile calibratedProfile(int windowCount) {
        return new StyleCalibrationProfile(
                StyleModule.CALIBRATION_SCHEMA_VERSION,
                CanonicalJson.hash("target-corpus"),
                CONTRACT_HASH,
                StyleWindowConfiguration.defaults().configurationHash(),
                List.of(StyleCalibrationEngineTest.stratum(
                        StyleStratum.narration(), windowCount
                ))
        );
    }

    private StyleWindowScore score(
            StyleWindow window,
            StyleDistanceReport report,
            StyleCalibrationProfile profile
    ) {
        return scorer.score(window, report, profile);
    }

    private static StyleWindow window(
            StyleWindowKind kind,
            String shiftReason
    ) {
        return window(
                kind,
                List.of(Ids.BlockId.create(), Ids.BlockId.create()),
                shiftReason
        );
    }

    private static StyleWindow window(
            StyleWindowKind kind,
            List<Ids.BlockId> blockIds,
            String shiftReason
    ) {
        return StyleWindow.create(
                kind,
                0,
                StyleStratum.narration(),
                "unknown",
                "narration",
                blockIds,
                kind == StyleWindowKind.MICRO ? 750 : 3_000,
                true,
                shiftReason
        );
    }

    private static StyleDistanceReport highReport() {
        return report(Map.of(
                StyleFeatureChannel.SURFACE, new BigDecimal("0.2"),
                StyleFeatureChannel.GRAMMAR, new BigDecimal("0.2"),
                StyleFeatureChannel.RHYTHM, new BigDecimal("0.05"),
                StyleFeatureChannel.NARRATIVE, new BigDecimal("0.05"),
                StyleFeatureChannel.LEXICAL, new BigDecimal("0.05")
        ));
    }

    private static StyleDistanceReport topicOnlyReport() {
        return report(Map.of(
                StyleFeatureChannel.SURFACE, new BigDecimal("0.2"),
                StyleFeatureChannel.GRAMMAR, new BigDecimal("0.05"),
                StyleFeatureChannel.RHYTHM, new BigDecimal("0.05"),
                StyleFeatureChannel.NARRATIVE, new BigDecimal("0.05"),
                StyleFeatureChannel.LEXICAL, new BigDecimal("0.05")
        ));
    }

    private static StyleDistanceReport normalReport() {
        return report(Map.of(
                StyleFeatureChannel.SURFACE, new BigDecimal("0.05"),
                StyleFeatureChannel.GRAMMAR, new BigDecimal("0.05"),
                StyleFeatureChannel.RHYTHM, new BigDecimal("0.05"),
                StyleFeatureChannel.NARRATIVE, new BigDecimal("0.05"),
                StyleFeatureChannel.LEXICAL, new BigDecimal("0.05")
        ));
    }

    private static StyleDistanceReport report(
            Map<StyleFeatureChannel, BigDecimal> distances
    ) {
        List<StyleChannelDistance> channels = new ArrayList<>();
        for (StyleFeatureChannel channel : StyleFeatureChannel.values()) {
            if (channel.required()) {
                channels.add(new StyleChannelDistance(
                        channel,
                        channel.featureVersion(),
                        channel.primaryMetric(),
                        distances.get(channel),
                        true,
                        Map.of()
                ));
            }
        }
        return new StyleDistanceReport(
                CONTRACT_HASH,
                CanonicalJson.hash("target"),
                CanonicalJson.hash("current"),
                channels,
                true
        );
    }
}
