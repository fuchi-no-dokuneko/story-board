package dev.storyblock.rewrite.policy;
import dev.storyblock.domain.Ids;
import dev.storyblock.style.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

record RewriteFindingRange(List<Ids.BlockId> affected, List<StyleAnomalyDecision> decisions) {
  static RewriteFindingRange evaluate(StyleAnalysisJob analysis, List<StyleAnalysisWindowFinding> selectedFindings) {
    Map<Ids.BlockId, Integer> snapshotOrdinals = new HashMap<>();
    List<StyleAnalysisBlock> snapshotBlocks = analysis.snapshot().blocks();
    for (int index = 0; index < snapshotBlocks.size(); index++) {
      snapshotOrdinals.put(snapshotBlocks.get(index).block().id(), index);
    }
    LinkedHashSet<Ids.BlockId> selectedBlocks = new LinkedHashSet<>();
    List<StyleAnomalyDecision> decisions = new ArrayList<>();
    for (StyleAnalysisWindowFinding finding : selectedFindings) {
      StyleAnomalyDecision decision = RewriteEligibilityPolicyDecision.decision(finding);
      if (!finding.windowId().equals(decision.operationalWindowId())
          || !finding.canTriggerRewrite()
          || finding.decisionState() != decision.state()
          || finding.confidence() != decision.confidence()) {
        throw new RewriteEligibilityException(
            "Rewrite finding does not match its immutable anomaly decision"
        );
      }
      for (Ids.BlockId blockId : finding.blockIds()) {
        if (!snapshotOrdinals.containsKey(blockId)) {
          throw new RewriteEligibilityException(
              "Rewrite finding references a block outside the analysis snapshot"
          );
        }
        selectedBlocks.add(blockId);
      }
      decisions.add(decision);
    }
    int first = selectedBlocks.stream().mapToInt(snapshotOrdinals::get).min()
        .orElseThrow();
    int last = selectedBlocks.stream().mapToInt(snapshotOrdinals::get).max()
        .orElseThrow();
    List<Ids.BlockId> affected = snapshotBlocks.subList(first, last + 1).stream()
        .map(block -> block.block().id()).toList();
    if (affected.size() > dev.storyblock.rewrite.RewriteModule.MAX_EDITABLE_BLOCKS) {
      throw new RewriteEligibilityException(
          "Rewrite findings do not resolve to a minimal bounded range"
      );
    }
    return new RewriteFindingRange(affected, decisions);
  }
}
