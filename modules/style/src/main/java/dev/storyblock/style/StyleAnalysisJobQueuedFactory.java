package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import java.time.Instant;

final class StyleAnalysisJobQueuedFactory {
    static StyleAnalysisJob queued(Ids.JobId jobId, Ids.StyleAnalysisId analysisId, StyleAnalysisSnapshot snapshot, int maxAttempts, String idempotencyKey, String requestHash, AuditContext auditContext, Instant retentionUntil, Instant createdAt)  {
        return new StyleAnalysisJob(
                jobId,
                analysisId,
                snapshot,
                StyleAnalysisJobStatus.QUEUED,
                null,
                null,
                0,
                maxAttempts,
                idempotencyKey,
                requestHash,
                null,
                null,
                null,
                auditContext,
                retentionUntil,
                createdAt,
                createdAt
        );
    }
}
