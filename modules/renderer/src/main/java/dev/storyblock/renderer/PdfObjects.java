package dev.storyblock.renderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfObjects {
  static void object(
      ByteArrayOutputStream output,
      long[] offsets,
      int number,
      String body
  ) throws IOException {
    offsets[number] = output.size();
    ascii(output, number + " 0 obj\n" + body + "\nendobj\n");
  }

  static void streamObject(
      ByteArrayOutputStream output,
      long[] offsets,
      int number,
      String dictionary,
      byte[] content
  ) throws IOException {
    offsets[number] = output.size();
    ascii(output, number + " 0 obj\n" + dictionary + "\nstream\n");
    output.write(content);
    ascii(output, "\nendstream\nendobj\n");
  }

  static void ascii(ByteArrayOutputStream output, String value)
      throws IOException {
    output.write(value.getBytes(StandardCharsets.US_ASCII));
  }
}
