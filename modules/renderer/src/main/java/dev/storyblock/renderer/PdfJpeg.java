package dev.storyblock.renderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

final class PdfJpeg {
  static byte[] encode(BufferedImage page) {
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

}
