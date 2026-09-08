package dev.storyblock.renderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfPageComposer {
  final Font bodyFont;
  final Font chapterFont;
  final Font titleFont;
  final List<BufferedImage> pages = new ArrayList<>();
  BufferedImage page;
  Graphics2D graphics;
  int cursorY;

  PdfPageComposer(Font bodyFont, Font chapterFont, Font titleFont) {
    this.bodyFont = bodyFont;
    this.chapterFont = chapterFont;
    this.titleFont = titleFont;
    newPage();
  }

  void cover(String title, String revisionId) {
        PdfPageComposerCoverAction.cover(this, title, revisionId);
    }

  void chapter(String text) {
        PdfPageComposerChapterAction.chapter(this, text);
    }

  void scene(String text) {
    ensureSpace(50);
    graphics.setFont(bodyFont.deriveFont(Font.BOLD, 22f));
    graphics.setColor(new Color(70, 79, 91));
    graphics.drawString(text, MARGIN_X, cursorY + 26);
    cursorY += 42;
  }

  void paragraph(String text) {
        PdfPageComposerParagraphAction.paragraph(this, text);
    }

  void image(BufferedImage source) {
        PdfPageComposerImageAction.image(this, source);
    }

  void caption(String text) {
        PdfPageComposerCaptionAction.caption(this, text);
    }

  List<BufferedImage> finish() {
        return PdfPageComposerFinishAction.finish(this);
    }

  void ensureSpace(int height) {
    if (cursorY + height > PAGE_HEIGHT - MARGIN_BOTTOM) {
      newPage();
    }
  }

  void newPage() {
    if (graphics != null) {
      graphics.dispose();
    }
    page = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
    graphics = page.createGraphics();
    configure(graphics);
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
    pages.add(page);
    cursorY = MARGIN_TOP;
  }

  void drawCentered(String text, int baseline) {
    int x = (PAGE_WIDTH - graphics.getFontMetrics().stringWidth(text)) / 2;
    graphics.drawString(text, x, baseline);
  }

  static void configure(Graphics2D graphics) {
    graphics.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    );
    graphics.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
    );
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BICUBIC
    );
  }

  static int contentWidth() {
    return PAGE_WIDTH - 2 * MARGIN_X;
  }
}
