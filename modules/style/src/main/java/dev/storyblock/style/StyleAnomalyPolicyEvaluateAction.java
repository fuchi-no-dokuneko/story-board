package dev.storyblock.style;

import java.util.List;
import java.util.Objects;

final class StyleAnomalyPolicyEvaluateAction {
  static StyleAnomalyDecision evaluate(StyleAnomalyPolicy self, StyleWindowScore operational, List<StyleWindowScore> nonOverlap, List<StyleWindowScore> micro)  {
    Objects.requireNonNull(operational, "operational");
    nonOverlap = List.copyOf(nonOverlap);
    micro = List.copyOf(micro);
    if (!operational.window().primaryDecisionEligible()) {
      throw new IllegalArgumentException(
          "Style decisions require a full operational window"
      );
    }
    StyleAnomalyPolicyValidateSupportingWindows.validateSupportingWindows(operational, nonOverlap, micro);

    List<String> localized = micro.stream()
        .filter(StyleWindowScore::hasAnyAboveQ95)
        .map(score -> score.window().windowId())
        .toList();
    StyleCalibrationConfidence confidence = operational.profileSelection().confidence();
    if (!operational.profileSelection().calibrationAvailable()
        || confidence == StyleCalibrationConfidence.LOW_CONFIDENCE) {
      return StyleAnomalyPolicyDecision.decision(
          operational,
          StyleDecisionState.LOW_CONFIDENCE,
          StyleDecisionReason.INSUFFICIENT_CALIBRATION,
          confidence,
          List.of(),
          localized,
          false
      );
    }
    if (operational.surfaceOnlyAboveQ95()) {
      return StyleAnomalyPolicyDecision.decision(
          operational,
          StyleDecisionState.TOPIC_SHIFT_ONLY,
          StyleDecisionReason.TOKEN_CHANNEL_ONLY,
          confidence,
          List.of(),
          localized,
          false
      );
    }

    return CalibratedAnomaly.evaluate(operational, nonOverlap, localized, confidence);
  }
}
