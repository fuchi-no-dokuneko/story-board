package dev.storyblock.style;
import dev.storyblock.domain.CanonicalValues;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StyleAnalysisOutput {
  static StyleAnalysisExecution create(StyleAnalysisSnapshot snapshot, List<StyleAnalysisWindowFinding> findings, List<Map<String, Object>> decisions, List<StyleWindowFeatures> extracted, List<StyleWindowScore> scores) {
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
    );  }
}
