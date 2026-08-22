package dev.storyblock.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StyleCalibrationEngineTest {
    @Test
    void leaveOneOutCalibrationIsReproducibleAndSelfValidating() {
        StyleMaskingLexicon lexicon = StyleMaskingLexicon.empty();
        StyleFeatureContract contract = StyleFeatureContract.defaults(
                lexicon.vocabularyHash()
        );
        StyleFeatureAnalyzer analyzer = new StyleFeatureAnalyzer();
        List<StyleWindowFeatures> windows = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            NarrativeBlock block = NarrativeBlock.create(
                    Ids.BlockId.create(),
                    OrderKey.rebalanced(index, 30),
                    "他" + "慢".repeat(index % 8 + 1) + "走過長廊。",
                    BlockMetadata.empty(),
                    Map.of()
            );
            StyleWindow window = StyleWindow.create(
                    StyleWindowKind.OPERATIONAL,
                    0,
                    StyleStratum.narration(),
                    "unknown",
                    "narration",
                    List.of(block.id()),
                    3_000,
                    true,
                    null
            );
            windows.add(new StyleWindowFeatures(
                    window, analyzer.extract(List.of(block), lexicon, contract)
            ));
        }
        String corpusHash = CanonicalJson.hash("fixed-target-corpus");
        StyleCalibrationEngine engine = new StyleCalibrationEngine();

        StyleCalibrationProfile first = engine.calibrate(
                corpusHash, StyleWindowConfiguration.defaults(), windows
        );
        Collections.reverse(windows);
        StyleCalibrationProfile second = engine.calibrate(
                corpusHash, StyleWindowConfiguration.defaults(), windows
        );

        assertEquals(first, second);
        assertEquals(first.calibrationHash(), second.calibrationHash());
        StyleStratumCalibration narration = first.find(StyleStratum.narration())
                .orElseThrow();
        assertEquals(30, narration.windowCount());
        assertEquals(StyleCalibrationConfidence.CALIBRATED, narration.confidence());
        narration.channels().forEach(channel -> {
            assertEquals(30, channel.referenceDistances().size());
            assertTrue(channel.q95().compareTo(channel.q99()) <= 0);
        });
        assertEquals(
                first,
                StyleCalibrationProfile.fromCanonical(first.canonicalValue())
        );
    }

    @Test
    void insufficientSpeakerCorpusFallsBackToCalibratedGlobalDialogue() {
        StyleCalibrationProfile profile = profile(List.of(
                stratum(StyleStratum.dialogue("speaker_a"), 10),
                stratum(StyleStratum.dialogue(), 30)
        ));

        StyleProfileSelection selection = new StyleStratumSelector().select(
                StyleStratum.dialogue("speaker_a"), profile
        );

        assertEquals(StyleSelectionReason.SPEAKER_FALLBACK, selection.reason());
        assertEquals(StyleStratum.dialogue(), selection.selectedStratum());
        assertEquals(StyleCalibrationConfidence.CALIBRATED, selection.confidence());
    }

    static StyleCalibrationProfile profile(List<StyleStratumCalibration> strata) {
        return new StyleCalibrationProfile(
                StyleModule.CALIBRATION_SCHEMA_VERSION,
                CanonicalJson.hash("target-corpus"),
                CanonicalJson.hash("feature-contract"),
                StyleWindowConfiguration.defaults().configurationHash(),
                strata
        );
    }

    static StyleStratumCalibration stratum(StyleStratum stratum, int count) {
        List<BigDecimal> distances = new ArrayList<>();
        if (count > 1) {
            for (int index = 0; index < count; index++) {
                distances.add(new BigDecimal("0.1"));
            }
        }
        return new StyleStratumCalibration(
                stratum,
                count,
                StyleFeatureChannel.requiredChannels().stream()
                        .sorted(java.util.Comparator.comparing(Enum::ordinal))
                        .map(channel -> StyleChannelCalibration.fromDistances(
                                channel, distances
                        )).toList()
        );
    }
}
