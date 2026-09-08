package dev.storyblock.rewrite.policy;
import java.util.List;

final class RewriteRiskClassification {
  static RewriteRiskState classify(List<RewriteFactDifference> differences, List<RewriteNearCopyFinding> nearCopyFindings, java.util.Set<String> manualReasons) {
    boolean blocked = differences.stream().anyMatch(value ->
        value.disposition() == RewriteFactDisposition.BLOCK)
        || nearCopyFindings.stream().anyMatch(value ->
        value.disposition() == NearCopyDisposition.BLOCK);
    boolean manual = !manualReasons.isEmpty()
        || differences.stream().anyMatch(value ->
        value.disposition() == RewriteFactDisposition.MANUAL_ONLY)
        || nearCopyFindings.stream().anyMatch(value ->
        value.disposition() == NearCopyDisposition.MANUAL_ONLY);
    return blocked ? RewriteRiskState.BLOCKED : manual ? RewriteRiskState.MANUAL_ONLY : RewriteRiskState.SAFE;
  }
}
