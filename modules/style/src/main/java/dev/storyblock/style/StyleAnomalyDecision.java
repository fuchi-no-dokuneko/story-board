package dev.storyblock.style;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record StyleAnomalyDecision(
    StyleDecisionState state,
    StyleDecisionReason reason,
    StyleCalibrationConfidence confidence,
    String operationalWindowId,
    List<StyleFeatureChannel> independentQ99Channels,
    List<String> sustainingWindowIds,
    List<String> localizedMicroWindowIds,
    boolean intentionalShiftAdjusted,
    boolean canTriggerRewrite
) {
  public StyleAnomalyDecision {
    Objects.requireNonNull(state, "state");
    Objects.requireNonNull(reason, "reason");
    Objects.requireNonNull(confidence, "confidence");
    if (operationalWindowId == null
        || !operationalWindowId.matches("sha256:[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Style decision window ID is invalid");
    }
    independentQ99Channels = List.copyOf(independentQ99Channels);
    if (Set.copyOf(independentQ99Channels).size()
        != independentQ99Channels.size()
        || independentQ99Channels.stream().anyMatch(channel -> !channel.required())
        || !independentQ99Channels.stream()
            .sorted(java.util.Comparator.comparing(Enum::ordinal))
            .toList().equals(independentQ99Channels)) {
      throw new IllegalArgumentException(
          "Style decision independent q99 channels are invalid"
      );
    }
    sustainingWindowIds = StyleAnomalyDecisionUniqueHashes.uniqueHashes(sustainingWindowIds, "sustaining");
    localizedMicroWindowIds = StyleAnomalyDecisionUniqueHashes.uniqueHashes(localizedMicroWindowIds, "micro");
    if (canTriggerRewrite != (state == StyleDecisionState.REWRITE_CANDIDATE)
        || (confidence == StyleCalibrationConfidence.LOW_CONFIDENCE
        && canTriggerRewrite)
        || (state == StyleDecisionState.TOPIC_SHIFT_ONLY && canTriggerRewrite)) {
      throw new IllegalArgumentException(
          "Style rewrite eligibility does not match decision state"
      );
    }
    if (state == StyleDecisionState.REWRITE_CANDIDATE
        && (reason != StyleDecisionReason.SUSTAINED_MULTI_CHANNEL_Q99
        || confidence != StyleCalibrationConfidence.CALIBRATED
        || independentQ99Channels.size() < 2
        || sustainingWindowIds.size() < 2
        || intentionalShiftAdjusted)) {
      throw new IllegalArgumentException(
          "Rewrite candidate lacks calibrated sustained q99 evidence"
      );
    }
  }

  public static StyleAnomalyDecision fromCanonical(Map<String, Object> value) {
    return StyleAnomalyDecisionFromCanonicalFactory.fromCanonical(value);
  }

  public Map<String, Object> canonicalValue() {
    return StyleAnomalyDecisionCanonicalValueAction.canonicalValue(this);
  }

}
