package dev.storyblock.style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class StyleAnalysisExecutorExecuteAction {
  static StyleAnalysisExecution execute(StyleAnalysisExecutor self, StyleAnalysisSnapshot snapshot)  {
    Objects.requireNonNull(snapshot, "snapshot");
    StyleProfileVersionContent content = snapshot.profileVersion().content();
    StyleCalibrationProfile calibration = content.calibrationProfile()
        .orElseThrow(() -> new IllegalArgumentException(
            "Style analysis requires persisted calibration statistics"
        ));
    StyleFeatureSet baseline = StyleAnalysisExecutorRequiredBaseline.requiredBaseline(content.featureSet());
    List<StyleWindowFeatures> extracted = self.rollingAnalyzer.analyze(
        snapshot.blocks(),
        snapshot.maskingLexicon(),
        baseline.contract(),
        content.windowConfiguration()
    );
    List<StyleWindowScore> scores = extracted.stream().map(current ->
        self.scorer.score(
            current.window(),
            self.featureAnalyzer.compare(baseline, current.featureSet()),
            calibration
        )
    ).toList();

    List<StyleAnalysisWindowFinding> findings = new ArrayList<>();
    List<Map<String, Object>> decisions = new ArrayList<>();
    for (StyleWindowScore operational : scores.stream()
        .filter(score -> score.window().primaryDecisionEligible()).toList()) {
      List<StyleWindowScore> nonOverlap = scores.stream()
          .filter(score -> score.window().sustainmentEligible())
          .filter(score -> StyleAnalysisExecutorSameContext.sameContext(operational, score))
          .toList();
      List<StyleWindowScore> micro = scores.stream()
          .filter(score -> score.window().localizationOnly())
          .filter(score -> StyleAnalysisExecutorSameContext.sameContext(operational, score))
          .toList();
      StyleAnomalyDecision decision = self.policy.evaluate(
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

    return StyleAnalysisOutput.create(snapshot, findings, decisions, extracted, scores);
  }
}
