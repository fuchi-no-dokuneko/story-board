package dev.storyblock.renderer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfPageComposerImageAction {
    static void image(PdfPageComposer self, BufferedImage source)  {
        double scale = Math.min(
            (double) PdfPageComposer.contentWidth() / source.getWidth(),
            (double) MAX_IMAGE_HEIGHT / source.getHeight()
        );
        scale = Math.min(1.0d, scale);
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        self.ensureSpace(height + 24);
        int x = (PAGE_WIDTH - width) / 2;
        self.graphics.setColor(new Color(225, 228, 232));
        self.graphics.fillRect(x - 2, self.cursorY - 2, width + 4, height + 4);
        self.graphics.drawImage(source, x, self.cursorY, width, height, Color.WHITE, null);
        self.cursorY += height + 20;
    }
}
