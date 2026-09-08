package dev.storyblock.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfPageComposerChapterAction {
    static void chapter(PdfPageComposer self, String text)  {
        self.ensureSpace(70);
        self.graphics.setFont(self.chapterFont);
        self.graphics.setColor(new Color(27, 38, 55));
        for (String line : PdfTextWrapping.wrap(text, self.graphics.getFontMetrics(), PdfPageComposer.contentWidth())) {
          self.graphics.drawString(line, MARGIN_X, self.cursorY + 34);
          self.cursorY += 42;
        }
        self.graphics.setColor(new Color(91, 117, 139));
        self.graphics.setStroke(new BasicStroke(2f));
        self.graphics.drawLine(MARGIN_X, self.cursorY + 4, PAGE_WIDTH - MARGIN_X, self.cursorY + 4);
        self.cursorY += 28;
    }
}
