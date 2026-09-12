package dev.storyblock.style;

import dev.storyblock.domain.Ids;
import java.time.Instant;
import static dev.storyblock.style.StyleAnalysisJob.OWNER;
import static dev.storyblock.style.StyleAnalysisJob.FAILURE;

final class StyleAnalysisJobValidateState {
    static void validateState(
            StyleAnalysisJobStatus status,
            String leaseOwner,
            Instant leaseUntil,
            int attempt,
            Ids.ArtifactId resultArtifactId,
            String resultHash,
            String failureCode,
            Instant updatedAt
    ) {
        boolean leased = leaseOwner != null || leaseUntil != null;
        boolean completed = resultArtifactId != null || resultHash != null;
        if (status == StyleAnalysisJobStatus.RUNNING) {
            if (leaseOwner == null || !OWNER.matcher(leaseOwner).matches()
                    || leaseUntil == null || !leaseUntil.isAfter(updatedAt)
                    || attempt < 1 || completed || failureCode != null) {
                throw new IllegalArgumentException("Running style analysis state is invalid");
            }
            return;
        }
        if (leased) {
            throw new IllegalArgumentException("Non-running style analysis cannot hold a lease");
        }
        if (status == StyleAnalysisJobStatus.QUEUED) {
            if (attempt != 0 || completed || failureCode != null) {
                throw new IllegalArgumentException("Queued style analysis state is invalid");
            }
        } else if (status == StyleAnalysisJobStatus.SUCCEEDED) {
            if (attempt < 1 || resultArtifactId == null) {
                throw new IllegalArgumentException("Succeeded style analysis lacks a result");
            }
            StyleAnalysisJobRequireHash.requireHash(resultHash, "result");
            if (failureCode != null) {
                throw new IllegalArgumentException("Succeeded style analysis has a failure");
            }
        } else if (status == StyleAnalysisJobStatus.FAILED
                && (attempt < 1 || failureCode == null
                || !FAILURE.matcher(failureCode).matches() || completed)) {
            throw new IllegalArgumentException("Failed style analysis state is invalid");
        }
    }
}
