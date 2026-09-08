package dev.storyblock.style;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

final class StyleAnalysisTraceCompress {
    static byte[] compress(byte[] value) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(value);
            }
            return output.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException("Could not compress style trace", failure);
        }
    }
}
