package dev.storyblock.style;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import static dev.storyblock.style.StyleAnalysisCompletionCommand.*;

final class StyleAnalysisCompletionCommandValidation {
  static void validate(StyleAnalysisSummary summary, List<StyleAnalysisWindowFinding> windows, StyleAnalysisTrace trace, String idempotencyKey, Instant completedAt) {
    if (windows.size() != summary.operationalWindowCount()
            || windows.size() > StyleAnalysisSnapshot.MAX_BLOCKS
            || new HashSet<>(windows.stream().map(
                StyleAnalysisWindowFinding::windowId
            ).toList()).size() != windows.size()) {
          throw new IllegalArgumentException(
              "Style completion windows do not match the summary"
          );
        }
        EnumMap<StyleDecisionState, Integer> decisions = new EnumMap<>(
            StyleDecisionState.class
        );
        for (int index = 0; index < windows.size(); index++) {
          StyleAnalysisWindowFinding window = windows.get(index);
          if (window.ordinal() != index) {
            throw new IllegalArgumentException(
                "Style completion window ordinals must be contiguous"
            );
          }
          decisions.merge(window.decisionState(), 1, Integer::sum);
        }
        for (StyleDecisionState state : StyleDecisionState.values()) {
          if (summary.decisionCounts().get(state).intValue()
              != decisions.getOrDefault(state, 0).intValue()) {
            throw new IllegalArgumentException(
                "Style completion decision totals do not match windows"
            );
          }
        }
        Objects.requireNonNull(trace, "trace");
        if (idempotencyKey == null || idempotencyKey.isBlank()
            || idempotencyKey.length() > 200) {
          throw new IllegalArgumentException(
              "Style completion idempotency key is invalid"
          );
        }
        Objects.requireNonNull(completedAt, "completedAt");
        if (!trace.createdAt().equals(completedAt)) {
          throw new IllegalArgumentException(
              "Style trace and completion timestamps must match"
          );
        }
  }
}
