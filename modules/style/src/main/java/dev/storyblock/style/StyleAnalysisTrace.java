package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public record StyleAnalysisTrace(
        Ids.StyleAnalysisId analysisId,
        Ids.ArtifactId artifactId,
        String contentHash,
        byte[] compressedContent,
        int uncompressedBytes,
        Instant createdAt,
        Instant expiresAt
) {
    public static final String KIND = "style-analysis-trace";
    public static final String MEDIA_TYPE = "application/vnd.storyblock.style-trace+json";
    public static final String CODEC = "gzip";
    public static final int MAX_COMPRESSED_BYTES = 2 * 1024 * 1024;
    public static final int MAX_UNCOMPRESSED_BYTES = 16 * 1024 * 1024;
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public StyleAnalysisTrace {
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(artifactId, "artifactId");
        if (contentHash == null || !HASH.matcher(contentHash).matches()) {
            throw new IllegalArgumentException("Style trace content hash is invalid");
        }
        compressedContent = Objects.requireNonNull(
                compressedContent, "compressedContent"
        ).clone();
        if (compressedContent.length < 2
                || compressedContent.length > MAX_COMPRESSED_BYTES
                || uncompressedBytes < 2
                || uncompressedBytes > MAX_UNCOMPRESSED_BYTES
                || !CanonicalJson.hashBytes(compressedContent).equals(contentHash)
                || !artifactId.equals(Ids.ArtifactId.derive(analysisId, contentHash))) {
            throw new IllegalArgumentException("Style trace content metadata is invalid");
        }
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Style trace expiry must follow creation");
        }
        byte[] expanded = decompress(compressedContent, uncompressedBytes);
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = CanonicalJson.mapper().readValue(expanded, Map.class);
        if (!MessageDigest.isEqual(expanded, CanonicalJson.bytes(parsed))) {
            throw new IllegalArgumentException("Style trace JSON is not canonical");
        }
    }

    public static StyleAnalysisTrace create(
            Ids.StyleAnalysisId analysisId,
            Map<String, Object> trace,
            Instant createdAt,
            Instant expiresAt
    ) {
        byte[] canonical = CanonicalJson.bytes(CanonicalValues.freezeMap(
                trace, "style_analysis_trace"
        ));
        byte[] compressed = compress(canonical);
        String hash = CanonicalJson.hashBytes(compressed);
        return new StyleAnalysisTrace(
                analysisId,
                Ids.ArtifactId.derive(analysisId, hash),
                hash,
                compressed,
                canonical.length,
                createdAt,
                expiresAt
        );
    }

    public static StyleAnalysisTrace fromCompressed(
            Ids.StyleAnalysisId analysisId,
            String contentHash,
            byte[] compressedContent,
            int uncompressedBytes,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new StyleAnalysisTrace(
                analysisId,
                Ids.ArtifactId.derive(analysisId, contentHash),
                contentHash,
                compressedContent,
                uncompressedBytes,
                createdAt,
                expiresAt
        );
    }

    @Override
    public byte[] compressedContent() {
        return compressedContent.clone();
    }

    public Map<String, Object> metadataValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("artifact_id", artifactId.value());
        value.put("codec", CODEC);
        value.put("compressed_bytes", compressedContent.length);
        value.put("content_hash", contentHash);
        value.put("expires_at", expiresAt.toString());
        value.put("media_type", MEDIA_TYPE);
        value.put("uncompressed_bytes", uncompressedBytes);
        return CanonicalValues.freezeMap(value, "style_analysis_trace_metadata");
    }

    private static byte[] compress(byte[] value) {
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

    private static byte[] decompress(byte[] value, int expectedBytes) {
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
