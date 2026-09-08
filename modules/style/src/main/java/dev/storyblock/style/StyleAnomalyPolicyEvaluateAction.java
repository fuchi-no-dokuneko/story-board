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

    long q95 = operational.independentAboveQ95();
    long q99 = operational.independentAboveQ99();
    List<String> sustaining = nonOverlap.stream()
        .filter(score -> score.profileSelection().confidence()
            == StyleCalibrationConfidence.CALIBRATED)
        .filter(score -> score.independentAboveQ99() >= 2)
        .map(score -> score.window().windowId())
        .toList();
    StyleDecisionState state;
    StyleDecisionReason reason;
    if (q99 >= 2 && sustaining.size() >= 2) {
      state = StyleDecisionState.REWRITE_CANDIDATE;
      reason = StyleDecisionReason.SUSTAINED_MULTI_CHANNEL_Q99;
    } else if (q95 >= 2) {
      state = StyleDecisionState.WARNING;
      reason = StyleDecisionReason.MULTI_CHANNEL_Q95;
    } else {
      state = StyleDecisionState.NORMAL;
      reason = StyleDecisionReason.WITHIN_CALIBRATED_RANGE;
    }

    boolean adjusted = operational.window().intentionalStyleShiftReason() != null
        && (state == StyleDecisionState.WARNING
        || state == StyleDecisionState.REWRITE_CANDIDATE);
    if (adjusted) {
      state = state == StyleDecisionState.REWRITE_CANDIDATE
          ? StyleDecisionState.WARNING : StyleDecisionState.NORMAL;
      reason = StyleDecisionReason.INTENTIONAL_STYLE_SHIFT;
    }
    return StyleAnomalyPolicyDecision.decision(
        operational,
        state,
        reason,
        confidence,
        sustaining,
        localized,
        adjusted
    );
  }
}
