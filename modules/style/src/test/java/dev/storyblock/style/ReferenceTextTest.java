package dev.storyblock.style;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReferenceTextTest {
    @Test void referenceIngestionPreservesLongSentencesAndUnpunctuatedTail() {
        String sentence = "他沿著河岸走過舊城，想起遠方的家人，".repeat(8) + "直到天亮。";
        String tail = "這封信還沒有寫完";
        var segments = ReferenceSegments.split(sentence + "\n" + tail);
        assertEquals(sentence + tail, segments.stream().map(ReferenceText::text).collect(java.util.stream.Collectors.joining()));
        assertTrue(segments.stream().anyMatch(segment -> segment.text().equals(sentence)));
        assertEquals(tail, segments.getLast().text());
    }

    @Test void styleComparisonIncludesNovelsBeyondOneThousandBlocks() {
        var text = IntStream.range(0, 1001).mapToObj(i -> new ReferenceText("第" + i + "頁的信寫完了。她把信收進抽屜。")) .toList();
        var analyzer = new StyleFeatureAnalyzer(); var lexicon = StyleMaskingLexicon.empty();
        var contract = StyleFeatureContract.defaults(lexicon.vocabularyHash());
        var all = analyzer.extract(text, lexicon, contract);
        var firstThousand = analyzer.extract(text.subList(0, 1000), lexicon, contract);
        assertNotEquals(firstThousand.sourceHash(), all.sourceHash());
        assertEquals(5, analyzer.compare(all, firstThousand).channels().size());
    }
}
