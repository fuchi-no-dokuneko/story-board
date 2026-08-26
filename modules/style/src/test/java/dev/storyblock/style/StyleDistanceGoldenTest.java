package dev.storyblock.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.OrderKey;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StyleDistanceGoldenTest {
    @Test
    void chineseNarrationAndCantoneseDialogueMatchTheGoldenMetrics() throws Exception {
        Map<String, Object> fixture;
        try (InputStream input = getClass().getResourceAsStream(
                "/golden/style-distance.json"
        )) {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = CanonicalJson.mapper().readValue(input, Map.class);
            fixture = parsed;
        }
        StyleMaskingLexicon lexicon = StyleMaskingLexicon.empty();
        StyleFeatureContract contract = StyleFeatureContract.defaults(
                lexicon.vocabularyHash()
        );
        StyleFeatureAnalyzer analyzer = new StyleFeatureAnalyzer();
        StyleFeatureSet target = analyzer.extract(
                List.of(block((String) fixture.get("target_text"), Map.of())),
                lexicon,
                contract
        );
        StyleFeatureSet current = analyzer.extract(
                List.of(block(
                        (String) fixture.get("current_text"),
                        Map.of("speech", Map.of("speaker_id", fixture.get("current_speaker")))
                )),
                lexicon,
                contract
        );

        StyleDistanceReport report = analyzer.compare(target, current);
        assertEquals(fixture.get("token_kl_diagnostic_only"), report.tokenKlDiagnosticOnly());
        @SuppressWarnings("unchecked")
        Map<String, String> expectedMetrics = (Map<String, String>) fixture.get(
                "primary_metrics"
        );
        for (StyleChannelDistance channel : report.channels()) {
            assertEquals(
                    expectedMetrics.get(channel.channel().canonicalName()),
                    channel.primaryMetric().canonicalName()
            );
        }
        assertTrue(current.require(StyleFeatureChannel.NARRATIVE)
                .distribution().containsKey("mode:dialogue"));
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
