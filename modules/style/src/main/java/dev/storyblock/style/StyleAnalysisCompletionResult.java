package dev.storyblock.style;

public record StyleAnalysisCompletionResult(
        StyleAnalysisJob job,
        StyleAnalysisResult result,
        boolean idempotentReplay
) {
    public StyleAnalysisCompletionResult {
        java.util.Objects.requireNonNull(job, "job");
        java.util.Objects.requireNonNull(result, "result");
        if (job.status() != StyleAnalysisJobStatus.SUCCEEDED
                || !job.analysisId().equals(result.analysisId())
                || !job.jobId().equals(result.jobId())
                || !job.resultHash().equals(result.resultHash())) {
            throw new IllegalArgumentException(
                    "Style completion result does not match its job"
            );
        }
    }
}
