package dev.storyblock.storage.sqlite;

import dev.storyblock.storage.StorageException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class GzipCheckpointCodec {
    static final String NAME = "gzip-v1";
    private static final int MAX_UNCOMPRESSED_BYTES = 256 * 1024 * 1024;

    private GzipCheckpointCodec() {
    }

    static byte[] compress(byte[] canonicalJson) {
        if (canonicalJson.length == 0 || canonicalJson.length > MAX_UNCOMPRESSED_BYTES) {
            throw new StorageException("Canonical checkpoint payload has an unsafe size");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(canonicalJson);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new StorageException("Could not compress revision checkpoint", exception);
        }
    }

    static byte[] decompress(byte[] compressed, int expectedBytes) {
        if (expectedBytes < 1 || expectedBytes > MAX_UNCOMPRESSED_BYTES) {
            throw new StorageException("Checkpoint declares an unsafe uncompressed size");
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream(expectedBytes)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                if (output.size() + read > expectedBytes) {
                    throw new StorageException("Checkpoint expands beyond its declared size");
                }
                output.write(buffer, 0, read);
            }
            if (output.size() != expectedBytes) {
                throw new StorageException("Checkpoint size does not match its declaration");
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new StorageException("Could not decompress revision checkpoint", exception);
        }
    }
}
