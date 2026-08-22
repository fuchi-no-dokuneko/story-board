package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.List;
import java.util.Map;

public record StyleAnalysisExecution(
        StyleAnalysisSummary summary,
        List<StyleAnalysisWindowFinding> windows,
        Map<String, Object> tracePayload
) {
    public StyleAnalysisExecution {
        java.util.Objects.requireNonNull(summary, "summary");
        windows = List.copyOf(windows);
        tracePayload = CanonicalValues.freezeMap(
                tracePayload, "style_analysis_execution.trace"
        );
        if (summary.operationalWindowCount() != windows.size()) {
            throw new IllegalArgumentException(
                    "Style analysis execution windows do not match summary"
            );
        }
    }
}
