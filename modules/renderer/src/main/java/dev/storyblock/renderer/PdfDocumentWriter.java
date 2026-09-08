package dev.storyblock.renderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfDocumentWriter {
  private static final byte[] BINARY_MARKER = {
      '%', (byte) 0xe2, (byte) 0xe3, (byte) 0xcf, (byte) 0xd3, '\n'
  };

  PdfDocumentWriter() {
  }

  static byte[] write(List<BufferedImage> pages) {
    try {
      List<byte[]> jpegs = pages.stream()
          .map(PdfDocumentWriter::jpeg)
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

      for (int index = 0; index < pages.size(); index++) {
        int pageObject = 3 + index * 3;
        int imageObject = pageObject + 1;
        int contentObject = pageObject + 2;
        object(
            output,
            offsets,
            pageObject,
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /XObject << /Im0 " + imageObject
                + " 0 R >> >> /Contents " + contentObject + " 0 R >>"
        );
        streamObject(
            output,
            offsets,
            imageObject,
            "<< /Type /XObject /Subtype /Image /Width " + PAGE_WIDTH
                + " /Height " + PAGE_HEIGHT
                + " /ColorSpace /DeviceRGB /BitsPerComponent 8 "
                + "/Filter /DCTDecode /Length " + jpegs.get(index).length
                + " >>",
            jpegs.get(index)
        );
        byte[] commands = (
            "q\n595 0 0 842 0 0 cm\n/Im0 Do\nQ\n"
        ).getBytes(StandardCharsets.US_ASCII);
        streamObject(
            output,
            offsets,
            contentObject,
            "<< /Length " + commands.length + " >>",
            commands
        );
      }

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

  static byte[] jpeg(BufferedImage page) {
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
    if (!writers.hasNext()) {
      throw new IllegalStateException("JVM does not provide a JPEG writer");
    }
    ImageWriter writer = writers.next();
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
      writer.setOutput(output);
      JPEGImageWriteParam parameters = new JPEGImageWriteParam(Locale.ROOT);
      parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      parameters.setCompressionQuality(0.86f);
      parameters.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
      parameters.setOptimizeHuffmanTables(false);
      writer.write(null, new IIOImage(page, null, null), parameters);
      output.flush();
      return bytes.toByteArray();
    } catch (IOException failure) {
      throw new IllegalStateException("PDF page JPEG encoding failed", failure);
    } finally {
      writer.dispose();
    }
  }

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
