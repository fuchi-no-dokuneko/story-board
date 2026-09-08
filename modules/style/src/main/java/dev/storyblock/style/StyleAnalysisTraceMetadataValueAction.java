package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.LinkedHashMap;
import java.util.Map;

final class StyleAnalysisTraceMetadataValueAction {
    static Map<String, Object> metadataValue(StyleAnalysisTrace self)  {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("artifact_id", self.artifactId().value());
        value.put("codec", StyleAnalysisTrace.CODEC);
        value.put("compressed_bytes", self.compressedContent().length);
        value.put("content_hash", self.contentHash());
        value.put("expires_at", self.expiresAt().toString());
        value.put("media_type", StyleAnalysisTrace.MEDIA_TYPE);
        value.put("uncompressed_bytes", self.uncompressedBytes());
        return CanonicalValues.freezeMap(value, "style_analysis_trace_metadata");
    }
}
