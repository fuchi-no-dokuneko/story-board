package dev.storyblock.style;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import static dev.storyblock.style.StyleAnalysisTrace.MAX_UNCOMPRESSED_BYTES;

final class StyleAnalysisTraceDecompress {
    static byte[] decompress(byte[] value, int expectedBytes) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(value))) {
            byte[] expanded = gzip.readNBytes(MAX_UNCOMPRESSED_BYTES + 1);
            if (expanded.length != expectedBytes || expanded.length > MAX_UNCOMPRESSED_BYTES
                    || gzip.read() != -1) {
                throw new IllegalArgumentException("Style trace expanded size is invalid");
            }
            return expanded;
        } catch (IOException failure) {
            throw new IllegalArgumentException("Style trace is not valid gzip", failure);
        }
    }
}
