package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

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
  static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

  public StyleAnalysisTrace {
    Objects.requireNonNull(analysisId, "analysisId");
    Objects.requireNonNull(artifactId, "artifactId");
    if (contentHash == null || !HASH.matcher(contentHash).matches()) {
      throw new IllegalArgumentException("Style trace content hash is invalid");
    }
    compressedContent = Objects.requireNonNull(
        compressedContent, "compressedContent"
    ).clone();
    StyleAnalysisTraceValidation.validate(analysisId, artifactId, contentHash, compressedContent, uncompressedBytes, createdAt, expiresAt);
  }

  public static StyleAnalysisTrace create(
      Ids.StyleAnalysisId analysisId,
      Map<String, Object> trace,
      Instant createdAt,
      Instant expiresAt
  ) {
    return StyleAnalysisTraceCreateFactory.create(analysisId, trace, createdAt, expiresAt);
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
    return StyleAnalysisTraceMetadataValueAction.metadataValue(this);
  }

}
