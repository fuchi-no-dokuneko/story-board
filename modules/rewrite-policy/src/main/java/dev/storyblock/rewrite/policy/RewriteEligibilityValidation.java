package dev.storyblock.rewrite.policy;

import dev.storyblock.domain.Ids;
import dev.storyblock.rewrite.RewriteModule;
import dev.storyblock.style.*;
import java.util.HashSet;
import java.util.List;
import static dev.storyblock.rewrite.policy.RewriteEligibility.*;

final class RewriteEligibilityValidation {
  static void validate(List<String> findingIds, List<Ids.BlockId> affectedBlockIds, List<StyleAnomalyDecision> decisions) {
    if (findingIds.isEmpty() || findingIds.size() > RewriteModule.MAX_FINDINGS
            || new HashSet<>(findingIds).size() != findingIds.size()
            || findingIds.stream().anyMatch(id -> !HASH.matcher(id).matches())
            || decisions.size() != findingIds.size()) {
          throw new IllegalArgumentException("Rewrite eligibility findings are invalid");
        }
        if (affectedBlockIds.isEmpty()
            || affectedBlockIds.size() > RewriteModule.MAX_EDITABLE_BLOCKS
            || new HashSet<>(affectedBlockIds).size() != affectedBlockIds.size()) {
          throw new IllegalArgumentException(
              "Rewrite eligibility affected blocks are invalid"
          );
        }
        for (int index = 0; index < decisions.size(); index++) {
          StyleAnomalyDecision decision = decisions.get(index);
          if (!findingIds.get(index).equals(decision.operationalWindowId())
              || decision.state() != StyleDecisionState.REWRITE_CANDIDATE
              || decision.reason()
              != StyleDecisionReason.SUSTAINED_MULTI_CHANNEL_Q99
              || decision.confidence()
              != StyleCalibrationConfidence.CALIBRATED
              || !decision.canTriggerRewrite()
              || decision.independentQ99Channels().size() < 2
              || decision.sustainingWindowIds().size() < 2
              || decision.intentionalShiftAdjusted()) {
            throw new IllegalArgumentException(
                "Rewrite eligibility contains an ineligible style decision"
            );
          }
        }
  }
}
