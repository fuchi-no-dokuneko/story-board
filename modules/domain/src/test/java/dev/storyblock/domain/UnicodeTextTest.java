package dev.storyblock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UnicodeTextTest {
    @Test
    void countsCombiningSequencesAndEmojiAsGraphemes() {
        assertEquals(1, UnicodeText.graphemeCount("e\u0301"));
        assertEquals(1, UnicodeText.graphemeCount("👨‍👩‍👧‍👦"));
        assertEquals(2, UnicodeText.graphemeCount("🇭🇰人"));
    }

    @Test
    void recognizesChineseAndCantoneseQuotedSentences() {
        TextAnalysis result = UnicodeText.analyze("阿琪話：「走啦！」志明點頭。 ");

        assertEquals(2, result.sentenceCount());
        assertTrue(result.complete());
        assertEquals(1, result.safeSplitAnchors().size());
        assertTrue(result.validBlockShape());
    }

    @Test
    void validatorRejectsIncompleteAndThreeSentenceBlocksWithSafeAnchors() {
        InvalidBlockTextException incomplete = assertThrows(
                InvalidBlockTextException.class,
                () -> UnicodeText.validateBlock("仲未講完")
        );
        InvalidBlockTextException threeSentences = assertThrows(
                InvalidBlockTextException.class,
                () -> UnicodeText.validateBlock("一。二。三。")
        );

        assertTrue(incomplete.violations().contains(BlockTextViolation.INCOMPLETE_SENTENCE));
        assertTrue(threeSentences.violations().contains(BlockTextViolation.INVALID_SENTENCE_COUNT));
        assertEquals(2, threeSentences.analysis().safeSplitAnchors().size());
        assertFalse(threeSentences.analysis().validBlockShape());
    }

    @Test
    void decimalPointIsNotTreatedAsASentenceBoundary() {
        TextAnalysis result = UnicodeText.validateBlock("溫度是 3.14 度。結果正常！");

        assertEquals(2, result.sentenceCount());
    }

    @Test
    void lineBreaksDoNotCountAsVisibleGraphemes() {
        assertEquals(4, UnicodeText.graphemeCount("一二\n三四"));
        assertEquals(4, UnicodeText.graphemes("一二\r\n三四").size());
    }

    @Test
    void authorAnchorMustBeAnInternalGraphemeOffset() {
        assertEquals(List.of(2), UnicodeText.analyze("甲乙丙丁。", List.of(2)).safeSplitAnchors());
        assertThrows(
                IllegalArgumentException.class,
                () -> UnicodeText.analyze("一句。", List.of(99))
        );
    }

    @Test
    void validatorRejectsEmptyAndMoreThanOneHundredGraphemes() {
        InvalidBlockTextException empty = assertThrows(
                InvalidBlockTextException.class,
                () -> UnicodeText.validateBlock("  ")
        );
        InvalidBlockTextException tooLong = assertThrows(
                InvalidBlockTextException.class,
                () -> UnicodeText.validateBlock("字".repeat(100) + "。")
        );

        assertEquals(List.of(BlockTextViolation.EMPTY), empty.violations());
        assertTrue(tooLong.violations().contains(BlockTextViolation.TOO_LONG));
        assertEquals(101, tooLong.analysis().graphemeCount());
    }
}
