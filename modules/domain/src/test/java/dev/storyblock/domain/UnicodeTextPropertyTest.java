package dev.storyblock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.Normalizer;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class UnicodeTextPropertyTest {
    @Property
    void combiningMarksNeverCreateASecondGrapheme(@ForAll("asciiLetters") char base) {
        assertEquals(1, UnicodeText.graphemeCount(base + "\u0301\u0327"));
    }

    @Property
    void reportedSentenceAnchorAlwaysSplitsBetweenWholeGraphemes(
            @ForAll("visibleGraphemes") List<String> body
    ) {
        String firstSentence = String.join("", body) + "。";
        TextAnalysis analysis = UnicodeText.analyze(firstSentence + "下一句完成了。 ");
        int anchor = analysis.safeSplitAnchors().getFirst();

        assertEquals(
                Normalizer.normalize(firstSentence, Normalizer.Form.NFC),
                String.join("", UnicodeText.graphemes(analysis.normalizedText()).subList(0, anchor))
        );
        assertTrue(anchor > 0);
    }

    @Provide
    Arbitrary<Character> asciiLetters() {
        return Arbitraries.chars().range('a', 'z');
    }

    @Provide
    Arbitrary<List<String>> visibleGraphemes() {
        return Arbitraries.of("字", "e\u0301", "👨‍👩‍👧‍👦", "🇭🇰", "A")
                .list()
                .ofMinSize(1)
                .ofMaxSize(20);
    }
}
