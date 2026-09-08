package dev.storyblock.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfPageComposerFinishAction {
    static List<BufferedImage> finish(PdfPageComposer self)  {
        if (self.page != null && self.cursorY <= MARGIN_TOP && self.pages.size() > 1) {
          self.pages.removeLast();
          self.graphics.dispose();
          self.page = null;
        }
        for (int index = 0; index < self.pages.size(); index++) {
          Graphics2D footer = self.pages.get(index).createGraphics();
          PdfPageComposer.configure(footer);
          footer.setFont(self.bodyFont.deriveFont(13f));
          footer.setColor(new Color(125, 130, 138));
          String number = Integer.toString(index + 1);
          footer.drawString(
              number,
              (PAGE_WIDTH - footer.getFontMetrics().stringWidth(number)) / 2,
              PAGE_HEIGHT - 28
          );
          footer.dispose();
        }
        return List.copyOf(self.pages);
    }
}
