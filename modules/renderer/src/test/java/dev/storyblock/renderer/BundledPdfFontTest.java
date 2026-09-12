package dev.storyblock.renderer;

import java.awt.Font;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BundledPdfFontTest {
    @Test void packagedFontRendersBothChineseScriptsAndLatinWithoutSystemFonts() {
        var font = BundledPdfFont.at(Font.PLAIN, 24);
        assertEquals("Noto Sans CJK TC", font.getFamily(java.util.Locale.ROOT));
        assertEquals(-1, font.canDisplayUpTo("繁體中文，简体中文。霧港的最後一封信 ABC 123"));
        assertEquals(font, DeterministicPdfRendererChooseFont.chooseFont("繁體中文 ABC", Font.PLAIN, 24));
    }
}
