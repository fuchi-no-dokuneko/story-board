package dev.storyblock.renderer;

import dev.storyblock.domain.UnicodeText;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfPageComposer {
  private final Font bodyFont;
  private final Font chapterFont;
  private final Font titleFont;
  private final List<BufferedImage> pages = new ArrayList<>();
  private BufferedImage page;
  private Graphics2D graphics;
  private int cursorY;

  PdfPageComposer(Font bodyFont, Font chapterFont, Font titleFont) {
    this.bodyFont = bodyFont;
    this.chapterFont = chapterFont;
    this.titleFont = titleFont;
    newPage();
  }

  void cover(String title, String revisionId) {
    graphics.setColor(new Color(27, 38, 55));
    graphics.setFont(titleFont);
    List<String> lines = wrap(title, graphics.getFontMetrics(), PAGE_WIDTH - 2 * MARGIN_X);
    int totalHeight = lines.size() * 58;
    int y = Math.max(250, (PAGE_HEIGHT - totalHeight) / 2);
    for (String line : lines) {
      drawCentered(line, y);
      y += 58;
    }
    graphics.setFont(bodyFont.deriveFont(14f));
    graphics.setColor(new Color(100, 108, 119));
    drawCentered("Revision " + revisionId, y + 34);
    newPage();
  }

  void chapter(String text) {
    ensureSpace(70);
    graphics.setFont(chapterFont);
    graphics.setColor(new Color(27, 38, 55));
    for (String line : wrap(text, graphics.getFontMetrics(), contentWidth())) {
      graphics.drawString(line, MARGIN_X, cursorY + 34);
      cursorY += 42;
    }
    graphics.setColor(new Color(91, 117, 139));
    graphics.setStroke(new BasicStroke(2f));
    graphics.drawLine(MARGIN_X, cursorY + 4, PAGE_WIDTH - MARGIN_X, cursorY + 4);
    cursorY += 28;
  }

  void scene(String text) {
    ensureSpace(50);
    graphics.setFont(bodyFont.deriveFont(Font.BOLD, 22f));
    graphics.setColor(new Color(70, 79, 91));
    graphics.drawString(text, MARGIN_X, cursorY + 26);
    cursorY += 42;
  }

  void paragraph(String text) {
    graphics.setFont(bodyFont);
    FontMetrics metrics = graphics.getFontMetrics();
    List<String> lines = wrap("　　" + text, metrics, contentWidth());
    int required = lines.size() * BODY_LEADING + 12;
    ensureSpace(required);
    graphics.setColor(new Color(32, 35, 40));
    for (String line : lines) {
      graphics.drawString(line, MARGIN_X, cursorY + metrics.getAscent());
      cursorY += BODY_LEADING;
    }
    cursorY += 12;
  }

  void image(BufferedImage source) {
    double scale = Math.min(
        (double) contentWidth() / source.getWidth(),
        (double) MAX_IMAGE_HEIGHT / source.getHeight()
    );
    scale = Math.min(1.0d, scale);
    int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
    int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
    ensureSpace(height + 24);
    int x = (PAGE_WIDTH - width) / 2;
    graphics.setColor(new Color(225, 228, 232));
    graphics.fillRect(x - 2, cursorY - 2, width + 4, height + 4);
    graphics.drawImage(source, x, cursorY, width, height, Color.WHITE, null);
    cursorY += height + 20;
  }

  void caption(String text) {
    graphics.setFont(bodyFont.deriveFont(16f));
    FontMetrics metrics = graphics.getFontMetrics();
    List<String> lines = wrap(text, metrics, contentWidth() - 40);
    ensureSpace(lines.size() * 25 + 20);
    graphics.setColor(new Color(84, 88, 96));
    for (String line : lines) {
      int x = (PAGE_WIDTH - metrics.stringWidth(line)) / 2;
      graphics.drawString(line, x, cursorY + metrics.getAscent());
      cursorY += 25;
    }
    cursorY += 20;
  }

  List<BufferedImage> finish() {
    if (page != null && cursorY <= MARGIN_TOP && pages.size() > 1) {
      pages.removeLast();
      graphics.dispose();
      page = null;
    }
    for (int index = 0; index < pages.size(); index++) {
      Graphics2D footer = pages.get(index).createGraphics();
      configure(footer);
      footer.setFont(bodyFont.deriveFont(13f));
      footer.setColor(new Color(125, 130, 138));
      String number = Integer.toString(index + 1);
      footer.drawString(
          number,
          (PAGE_WIDTH - footer.getFontMetrics().stringWidth(number)) / 2,
          PAGE_HEIGHT - 28
      );
      footer.dispose();
    }
    return List.copyOf(pages);
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

  static List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
    List<String> lines = new ArrayList<>();
    StringBuilder line = new StringBuilder();
    for (String grapheme : UnicodeText.graphemes(text)) {
      if (!line.isEmpty() && metrics.stringWidth(line + grapheme) > maxWidth) {
        lines.add(line.toString());
        line.setLength(0);
      }
      line.append(grapheme);
    }
    if (!line.isEmpty()) {
      lines.add(line.toString());
    }
    return lines.isEmpty() ? List.of("") : List.copyOf(lines);
  }

  static int contentWidth() {
    return PAGE_WIDTH - 2 * MARGIN_X;
  }
}
