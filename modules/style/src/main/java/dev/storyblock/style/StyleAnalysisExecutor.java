package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StyleAnalysisExecutor {
    private final StyleRollingWindowAnalyzer rollingAnalyzer;
    private final StyleFeatureAnalyzer featureAnalyzer;
    private final StyleWindowScorer scorer;
    private final StyleAnomalyPolicy policy;

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
        Objects.requireNonNull(snapshot, "snapshot");
        StyleProfileVersionContent content = snapshot.profileVersion().content();
        StyleCalibrationProfile calibration = content.calibrationProfile()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Style analysis requires persisted calibration statistics"
                ));
        StyleFeatureSet baseline = requiredBaseline(content.featureSet());
        List<StyleWindowFeatures> extracted = rollingAnalyzer.analyze(
                snapshot.blocks(),
                snapshot.maskingLexicon(),
                baseline.contract(),
                content.windowConfiguration()
        );
        List<StyleWindowScore> scores = extracted.stream().map(current ->
                scorer.score(
                        current.window(),
                        featureAnalyzer.compare(baseline, current.featureSet()),
                        calibration
                )
        ).toList();

        List<StyleAnalysisWindowFinding> findings = new ArrayList<>();
        List<Map<String, Object>> decisions = new ArrayList<>();
        for (StyleWindowScore operational : scores.stream()
                .filter(score -> score.window().primaryDecisionEligible()).toList()) {
            List<StyleWindowScore> nonOverlap = scores.stream()
                    .filter(score -> score.window().sustainmentEligible())
                    .filter(score -> sameContext(operational, score))
                    .toList();
            List<StyleWindowScore> micro = scores.stream()
                    .filter(score -> score.window().localizationOnly())
                    .filter(score -> sameContext(operational, score))
                    .toList();
            StyleAnomalyDecision decision = policy.evaluate(
                    operational, nonOverlap, micro
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("decision", decision.canonicalValue());
            StyleAnalysisWindowFinding finding = new StyleAnalysisWindowFinding(
                    findings.size(),
                    operational.window().windowId(),
                    operational.window().blockIds(),
                    decision.state(),
                    decision.confidence(),
                    decision.canTriggerRewrite(),
                    payload
            );
            findings.add(finding);
            decisions.add(decision.canonicalValue());
        }

        EnumMap<StyleDecisionState, Integer> counts = new EnumMap<>(
                StyleDecisionState.class
        );
        findings.forEach(finding -> counts.merge(
                finding.decisionState(), 1, Integer::sum
        ));
        int low = counts.getOrDefault(StyleDecisionState.LOW_CONFIDENCE, 0);
        StyleAnalysisSummary summary = new StyleAnalysisSummary(
                snapshot.blocks().size(),
                findings.size(),
                findings.size() - low,
                counts
        );

        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("analysis_schema_version", StyleModule.ANALYSIS_SCHEMA_VERSION);
        trace.put("decisions", decisions);
        trace.put("profile_version_hash", snapshot.profileVersionHash());
        trace.put("snapshot_hash", snapshot.snapshotHash());
        trace.put("summary", summary.canonicalValue());
        trace.put("window_features", extracted.stream().map(value -> Map.of(
                "feature_set", value.featureSet().canonicalValue(),
                "window", value.window().canonicalValue()
        )).toList());
        trace.put("window_scores", scores.stream()
                .map(StyleWindowScore::canonicalValue).toList());
        return new StyleAnalysisExecution(
                summary,
                findings,
                CanonicalValues.freezeMap(trace, "style_analysis_trace")
        );
    }

    private static StyleFeatureSet requiredBaseline(StyleFeatureSet source) {
        return new StyleFeatureSet(
                source.sourceHash(),
                source.contract(),
                source.channels().stream()
                        .filter(vector -> vector.channel().required())
                        .toList()
        );
    }

    private static boolean sameContext(
            StyleWindowScore operational,
            StyleWindowScore supporting
    ) {
        StyleWindow expected = operational.window();
        StyleWindow actual = supporting.window();
        return expected.segment() == actual.segment()
                && expected.requestedStratum().equals(actual.requestedStratum())
                && expected.pov().equals(actual.pov())
                && expected.narrativeMode().equals(actual.narrativeMode())
                && Objects.equals(
                        expected.intentionalStyleShiftReason(),
                        actual.intentionalStyleShiftReason()
                )
                && operational.profileSelection().selectedStratum().equals(
                        supporting.profileSelection().selectedStratum()
                );
    }
}
