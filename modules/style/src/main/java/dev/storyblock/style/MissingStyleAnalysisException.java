package dev.storyblock.style;

import dev.storyblock.domain.Ids;

public final class MissingStyleAnalysisException extends RuntimeException {
    public MissingStyleAnalysisException(Ids.StyleAnalysisId analysisId) {
        super("Style analysis does not exist: " + analysisId.value());
    }
}
