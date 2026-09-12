package dev.storyblock.style;

import java.util.Objects;

public final class StyleAnalysisExecutor {
    final StyleRollingWindowAnalyzer rollingAnalyzer;
    final StyleFeatureAnalyzer featureAnalyzer;
    final StyleWindowScorer scorer;
    final StyleAnomalyPolicy policy;

    public StyleAnalysisExecutor() {
        this(
                new StyleRollingWindowAnalyzer(),
                new StyleFeatureAnalyzer(),
                new StyleWindowScorer(),
                new StyleAnomalyPolicy()
        );
    }

    StyleAnalysisExecutor(
            StyleRollingWindowAnalyzer rollingAnalyzer,
            StyleFeatureAnalyzer featureAnalyzer,
            StyleWindowScorer scorer,
            StyleAnomalyPolicy policy
    ) {
        this.rollingAnalyzer = Objects.requireNonNull(
                rollingAnalyzer, "rollingAnalyzer"
        );
        this.featureAnalyzer = Objects.requireNonNull(
                featureAnalyzer, "featureAnalyzer"
        );
        this.scorer = Objects.requireNonNull(scorer, "scorer");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public StyleAnalysisExecution execute(StyleAnalysisSnapshot snapshot) {
        return StyleAnalysisExecutorExecuteAction.execute(this, snapshot);
    }

}
