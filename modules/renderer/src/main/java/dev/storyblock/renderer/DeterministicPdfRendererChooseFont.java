package dev.storyblock.renderer;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class DeterministicPdfRendererChooseFont {
    static Font chooseFont(String text, int style, int size) {
        Set<String> available = Set.of(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames(Locale.ROOT)
        );
        List<String> candidates = List.of(
                "Noto Serif CJK TC",
                "Noto Sans CJK TC",
                "Droid Sans Fallback",
                "AR PL KaitiM Big5",
                Font.SERIF,
                Font.DIALOG
        );
        Font best = new Font(Font.DIALOG, style, size);
        long bestScore = -1;
        for (String family : candidates) {
            if (!family.equals(Font.SERIF) && !family.equals(Font.DIALOG)
                    && !available.contains(family)) {
                continue;
            }
            Font candidate = new Font(family, style, size);
            long score = text.codePoints().filter(candidate::canDisplay).count();
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
            if (score == text.codePoints().count()) {
                return candidate;
            }
        }
        return best;
    }
}
