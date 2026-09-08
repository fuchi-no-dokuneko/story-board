package dev.storyblock.renderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static dev.storyblock.renderer.DeterministicPdfRenderer.*;

import static dev.storyblock.renderer.PdfObjects.*;

final class PdfPageObjects {
  static void writePages(ByteArrayOutputStream output, long[] offsets, List<byte[]> jpegs) throws IOException {
      for (int index = 0; index < jpegs.size(); index++) {
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

  }
}
