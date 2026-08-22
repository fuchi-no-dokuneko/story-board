package dev.storyblock.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StyleFeatureAnalyzerTest {
    private final StyleFeatureAnalyzer analyzer = new StyleFeatureAnalyzer();

    @Test
    void requiredChannelsAreVersionedDeterministicAndContentMasked() {
        StyleMaskingLexicon lexicon = new StyleMaskingLexicon(
                List.of("阿明"), List.of("香港")
        );
        StyleFeatureContract contract = StyleFeatureContract.defaults(
                lexicon.vocabularyHash()
        );
        List<NarrativeBlock> blocks = List.of(block(
                "阿明在香港走了123步。",
                Map.of("narrative_mode", "action", "actions", List.of("walk"))
        ));

        StyleFeatureSet first = analyzer.extract(blocks, lexicon, contract);
        StyleFeatureSet second = analyzer.extract(blocks, lexicon, contract);

        assertEquals(first, second);
        assertEquals(first.featureSetHash(), second.featureSetHash());
        assertTrue(first.channels().stream()
                .map(StyleFeatureVector::channel)
                .collect(java.util.stream.Collectors.toSet())
                .containsAll(StyleFeatureChannel.requiredChannels()));
        assertTrue(first.channels().stream().allMatch(vector ->
                vector.channelVersion().equals(vector.channel().featureVersion())
                        && vector.contractHash().equals(contract.contractHash())
        ));
        String surfaceKeys = first.require(StyleFeatureChannel.SURFACE)
                .distribution().keySet().toString();
        assertFalse(surfaceKeys.contains("阿明"));
        assertFalse(surfaceKeys.contains("香港"));
        assertFalse(surfaceKeys.contains("123"));
        assertTrue(surfaceKeys.contains("name") || surfaceKeys.contains("num"));
    }

    @Test
    void everyRequiredDistanceIsReportedAndKlCannotBecomeTheGate() {
        StyleMaskingLexicon lexicon = StyleMaskingLexicon.empty();
        StyleFeatureContract contract = StyleFeatureContract.defaults(
                lexicon.vocabularyHash()
        );
        StyleFeatureSet target = analyzer.extract(
                List.of(block("他慢慢走過長廊。", Map.of())),
                lexicon,
                contract,
                List.of(BigDecimal.ONE, BigDecimal.ZERO)
        );
        StyleFeatureSet current = analyzer.extract(
                List.of(block(
                        "她喊道：「快走！」",
                        Map.of("speech", Map.of("speaker_id", "char_a"))
                )),
                lexicon,
                contract,
                List.of(BigDecimal.ZERO, BigDecimal.ONE)
        );

        StyleDistanceReport report = analyzer.compare(target, current);

        assertTrue(report.tokenKlDiagnosticOnly());
        assertTrue(report.hasIndependentPrimaryEvidence());
        assertEquals(StyleFeatureChannel.values().length, report.channels().size());
        assertTrue(report.channels().stream().allMatch(distance ->
                !distance.primaryMetric().canonicalName().contains("kl")
        ));
        StyleChannelDistance surface = report.channels().stream()
                .filter(distance -> distance.channel() == StyleFeatureChannel.SURFACE)
                .findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, surface.diagnostics().get("token_kl_diagnostic_only"));
        assertTrue(surface.diagnostics().containsKey("kl_current_target"));
        assertTrue(surface.diagnostics().containsKey("kl_target_current"));
        assertNotEquals(BigDecimal.ZERO, report.channels().stream()
                .filter(distance -> distance.channel()
                        == StyleFeatureChannel.OPTIONAL_EMBEDDING)
                .findFirst().orElseThrow().primaryDistance());
    }

    @Test
    void apostrophesAndExplicitNoSpeechRemainNarration() {
        StyleMaskingLexicon lexicon = StyleMaskingLexicon.empty();
        StyleFeatureContract contract = StyleFeatureContract.defaults(
                lexicon.vocabularyHash()
        );

        StyleFeatureSet features = analyzer.extract(
                List.of(block(
                        "It's quiet and nobody's speaking.",
                        Map.of("speech", Map.of("type", "none"))
                )),
                lexicon,
                contract
        );

        Map<String, BigDecimal> narrative = features.require(
                StyleFeatureChannel.NARRATIVE
        ).distribution();
        assertTrue(narrative.containsKey("mode:description"));
        assertFalse(narrative.containsKey("mode:dialogue"));
    }

    private static NarrativeBlock block(String text, Map<String, Object> metadata) {
        return NarrativeBlock.create(
                Ids.BlockId.create(),
                OrderKey.initial(),
                text,
                new BlockMetadata(metadata),
                Map.of()
        );
    }
}
