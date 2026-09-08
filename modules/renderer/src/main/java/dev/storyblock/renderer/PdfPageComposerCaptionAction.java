package dev.storyblock.renderer;

import java.awt.Color;
import java.awt.FontMetrics;
import java.util.List;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfPageComposerCaptionAction {
    static void caption(PdfPageComposer self, String text)  {
        self.graphics.setFont(self.bodyFont.deriveFont(16f));
        FontMetrics metrics = self.graphics.getFontMetrics();
        List<String> lines = PdfTextWrapping.wrap(text, metrics, PdfPageComposer.contentWidth() - 40);
        self.ensureSpace(lines.size() * 25 + 20);
        self.graphics.setColor(new Color(84, 88, 96));
        for (String line : lines) {
          int x = (PAGE_WIDTH - metrics.stringWidth(line)) / 2;
          self.graphics.drawString(line, x, self.cursorY + metrics.getAscent());
          self.cursorY += 25;
        }
        self.cursorY += 20;
    }
}
