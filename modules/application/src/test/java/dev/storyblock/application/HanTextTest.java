package dev.storyblock.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class HanTextTest {
    @Test
    void countsHanUnicodeCodePointsAndExcludesPunctuationAndMetadata() {
        String text = "標題不計：春，夏。𠀀!";

        assertEquals("標題不計春夏𠀀", HanText.characters(text));
        assertEquals(7, HanText.count(text));
    }

    @Test
    void hashesOnlyTheNormalizedHanSequence() {
        String decorated = "春，夏。秋!";
        String plain = "春夏秋";

        assertEquals(HanText.sha256(plain), HanText.sha256(decorated));
        assertNotEquals(HanText.sha256("春夏"), HanText.sha256(decorated));
    }
}
