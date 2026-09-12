package dev.storyblock.renderer;

import java.awt.Color;
import java.awt.FontMetrics;
import java.util.List;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfPageComposerParagraphAction {
    static void paragraph(PdfPageComposer self, String text)  {
        self.graphics.setFont(self.bodyFont);
        FontMetrics metrics = self.graphics.getFontMetrics();
        List<String> lines = PdfTextWrapping.wrap("　　" + text, metrics, PdfPageComposer.contentWidth());
        int required = lines.size() * BODY_LEADING + 12;
        self.ensureSpace(required);
        self.graphics.setFont(self.bodyFont);
        self.graphics.setColor(new Color(32, 35, 40));
        for (String line : lines) {
          self.graphics.drawString(line, MARGIN_X, self.cursorY + metrics.getAscent());
          self.cursorY += BODY_LEADING;
        }
        self.cursorY += 12;
    }
}
