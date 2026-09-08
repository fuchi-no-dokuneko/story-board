package dev.storyblock.api.http;

import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisTrace;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import static dev.storyblock.api.http.StyleAnalysisFields.COMPRESSED_TRACE_FIELDS;

final class StyleAnalysisControllerParseTrace {
    static StyleAnalysisTrace parseTrace(
            StyleAnalysisJob job,
            Map<String, Object> trace,
            Instant completedAt
    ) {
        if (!trace.keySet().equals(COMPRESSED_TRACE_FIELDS)) {
            return StyleAnalysisTrace.create(
                    job.analysisId(), trace, completedAt, job.retentionUntil()
            );
        }
        String codec = StrictJsonRequest.string(
                trace, "codec", "style job result.trace"
        );
        if (!StyleAnalysisTrace.CODEC.equals(codec)) {
            throw new IllegalArgumentException(
                    "style job result.trace.codec must be gzip"
            );
        }
        final byte[] compressed;
        try {
            compressed = Base64.getDecoder().decode(StrictJsonRequest.string(
                    trace, "content_base64", "style job result.trace"
            ));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "style job result.trace.content_base64 is invalid", failure
            );
        }
        return StyleAnalysisTrace.fromCompressed(
                job.analysisId(),
                StrictJsonRequest.string(
                        trace, "content_hash", "style job result.trace"
                ),
                compressed,
                StrictJsonRequest.integer(
                        trace, "uncompressed_bytes", "style job result.trace"
                ),
                completedAt,
                job.retentionUntil()
        );
    }
}
