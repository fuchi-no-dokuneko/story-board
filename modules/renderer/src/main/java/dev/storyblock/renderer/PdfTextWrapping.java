package dev.storyblock.renderer;

import dev.storyblock.domain.UnicodeText;
import java.awt.FontMetrics;
import java.util.*;

final class PdfTextWrapping {
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

}
