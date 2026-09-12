package dev.storyblock.renderer;

import java.awt.Color;
import java.util.List;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfPageComposerCoverAction {
    static void cover(PdfPageComposer self, String title, String revisionId)  {
        self.graphics.setColor(new Color(27, 38, 55));
        self.graphics.setFont(self.titleFont);
        List<String> lines = PdfTextWrapping.wrap(title, self.graphics.getFontMetrics(), PAGE_WIDTH - 2 * MARGIN_X);
        int totalHeight = lines.size() * 58;
        int y = Math.max(250, (PAGE_HEIGHT - totalHeight) / 2);
        for (String line : lines) {
          self.drawCentered(line, y);
          y += 58;
        }
        self.graphics.setFont(self.bodyFont.deriveFont(14f));
        self.graphics.setColor(new Color(100, 108, 119));
        self.drawCentered("Revision " + revisionId, y + 34);
        self.newPage();
    }
}
