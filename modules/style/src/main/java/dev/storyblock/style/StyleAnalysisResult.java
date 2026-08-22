package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record StyleAnalysisResult(
        Ids.StyleAnalysisId analysisId,
        Ids.JobId jobId,
        StyleAnalysisSummary summary,
        Ids.ArtifactId traceArtifactId,
        String traceContentHash,
        int traceUncompressedBytes,
        Instant traceExpiresAt,
        String resultHash,
        Instant completedAt
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public StyleAnalysisResult {
        Objects.requireNonNull(analysisId, "analysisId");
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(traceArtifactId, "traceArtifactId");
        requireHash(traceContentHash, "trace");
        if (traceUncompressedBytes < 2
                || traceUncompressedBytes > StyleAnalysisTrace.MAX_UNCOMPRESSED_BYTES) {
            throw new IllegalArgumentException("Style result trace size is invalid");
        }
        Objects.requireNonNull(traceExpiresAt, "traceExpiresAt");
        requireHash(resultHash, "result");
        Objects.requireNonNull(completedAt, "completedAt");
        if (!traceExpiresAt.isAfter(completedAt)) {
            throw new IllegalArgumentException("Style result trace expiry is invalid");
        }
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("analysis_id", analysisId.value());
        value.put("completed_at", completedAt.toString());
        value.put("job_id", jobId.value());
        value.put("result_hash", resultHash);
        value.put("summary", summary.canonicalValue());
        value.put("trace_artifact_id", traceArtifactId.value());
        value.put("trace_content_hash", traceContentHash);
        value.put("trace_expires_at", traceExpiresAt.toString());
        value.put("trace_uncompressed_bytes", traceUncompressedBytes);
        return CanonicalValues.freezeMap(value, "style_analysis_result");
    }

    private static void requireHash(String value, String field) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException("Style " + field + " hash is invalid");
        }
    }
}
