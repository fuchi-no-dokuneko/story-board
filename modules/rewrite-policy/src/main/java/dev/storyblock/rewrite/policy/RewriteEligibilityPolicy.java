package dev.storyblock.rewrite.policy;

import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleProfileVersionView;
import java.time.Instant;
import java.util.List;

public final class RewriteEligibilityPolicy {
    public RewriteEligibility evaluate(
            StyleAnalysisJob analysis,
            StyleProfileVersionView currentProfile,
            List<StyleAnalysisWindowFinding> selectedFindings,
            Instant evaluatedAt
    ) {
        return RewriteEligibilityPolicyEvaluateAction.evaluate(this, analysis, currentProfile, selectedFindings, evaluatedAt);
    }

}
