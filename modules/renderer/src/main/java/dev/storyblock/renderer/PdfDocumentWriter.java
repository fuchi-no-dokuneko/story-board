package dev.storyblock.renderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

import static dev.storyblock.renderer.PdfObjects.*;

final class PdfDocumentWriter {
  private static final byte[] BINARY_MARKER = {
      '%', (byte) 0xe2, (byte) 0xe3, (byte) 0xcf, (byte) 0xd3, '\n'
  };

  PdfDocumentWriter() {
  }

  static byte[] write(List<BufferedImage> pages) {
    try {
      List<byte[]> jpegs = pages.stream()
          .map(PdfJpeg::encode)
          .toList();
      int objectCount = 2 + pages.size() * 3;
      long[] offsets = new long[objectCount + 1];
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ascii(output, "%PDF-1.4\n");
      output.write(BINARY_MARKER);
      object(output, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");

      StringBuilder kids = new StringBuilder("[");
      for (int index = 0; index < pages.size(); index++) {
        kids.append(3 + index * 3).append(" 0 R ");
      }
      kids.append(']');
      object(
          output,
          offsets,
          2,
          "<< /Type /Pages /Count " + pages.size() + " /Kids " + kids + " >>"
      );

      PdfPageObjects.writePages(output, offsets, jpegs);

      long xref = output.size();
      ascii(output, "xref\n0 " + (objectCount + 1) + "\n");
      ascii(output, "0000000000 65535 f \n");
      for (int object = 1; object <= objectCount; object++) {
        ascii(output, String.format(
            Locale.ROOT, "%010d 00000 n \n", offsets[object]
        ));
      }
      ascii(
          output,
          "trailer\n<< /Size " + (objectCount + 1)
              + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"
      );
      return output.toByteArray();
    } catch (IOException failure) {
      throw new IllegalStateException("PDF encoding failed", failure);
    }
  }

}
