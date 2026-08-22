package dev.storyblock.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.math.BigDecimal;
import java.util.ArrayList;
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
