package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import static dev.storyblock.style.StyleAnalysisTrace.*;

final class StyleAnalysisTraceValidation {
  static void validate(Ids.StyleAnalysisId analysisId, Ids.ArtifactId artifactId, String contentHash, byte[] compressedContent, int uncompressedBytes, Instant createdAt, Instant expiresAt) {
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
        byte[] expanded = StyleAnalysisTraceDecompress.decompress(compressedContent, uncompressedBytes);
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = CanonicalJson.mapper().readValue(expanded, Map.class);
        if (!MessageDigest.isEqual(expanded, CanonicalJson.bytes(parsed))) {
          throw new IllegalArgumentException("Style trace JSON is not canonical");
        }
  }
}
