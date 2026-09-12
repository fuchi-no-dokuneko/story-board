package dev.storyblock.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.Map;

final class StyleAnalysisTraceCreateFactory {
    static StyleAnalysisTrace create(Ids.StyleAnalysisId analysisId, Map<String, Object> trace, Instant createdAt, Instant expiresAt)  {
        byte[] canonical = CanonicalJson.bytes(CanonicalValues.freezeMap(
                trace, "style_analysis_trace"
        ));
        byte[] compressed = StyleAnalysisTraceCompress.compress(canonical);
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
}
