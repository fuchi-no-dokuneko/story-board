package dev.storyblock.style;

import dev.storyblock.domain.Ids;

public final class MissingStyleAnalysisJobException extends RuntimeException {
    public MissingStyleAnalysisJobException(Ids.JobId jobId) {
        super("Style analysis job does not exist: " + jobId.value());
    }
}
