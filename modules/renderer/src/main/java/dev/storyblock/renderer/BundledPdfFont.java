package dev.storyblock.renderer;

import java.awt.Font;
import java.util.Arrays;
import java.util.Locale;

final class BundledPdfFont {
    private static final class Loaded {
        static final Font FONT = load();
        private static Font load() {
            try (var input = BundledPdfFont.class.getResourceAsStream("/fonts/NotoSansCJK-Regular.ttc")) {
                if (input == null) throw new IllegalStateException("Bundled PDF font is missing");
                return Arrays.stream(Font.createFonts(input))
                        .filter(font -> font.getFamily(Locale.ROOT).equals("Noto Sans CJK TC"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("Traditional Chinese PDF font is missing"));
            } catch (Exception failure) {
                throw new IllegalStateException("Cannot load the bundled PDF font", failure);
            }
        }
    }
    static Font at(int style, int size) { return Loaded.FONT.deriveFont(style, (float) size); }
}
