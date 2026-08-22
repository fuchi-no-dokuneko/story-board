package dev.storyblock.style;

public record StyleAnalysisJobSaveResult(
        StyleAnalysisJob job,
        boolean idempotentReplay
) {
    public StyleAnalysisJobSaveResult {
        java.util.Objects.requireNonNull(job, "job");
    }
}
