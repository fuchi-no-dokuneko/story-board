package dev.storyblock.style;

import java.util.List;

public final class StyleAnomalyPolicy {
    public StyleAnomalyDecision evaluate(
            StyleWindowScore operational,
            List<StyleWindowScore> nonOverlap,
            List<StyleWindowScore> micro
    ) {
        return StyleAnomalyPolicyEvaluateAction.evaluate(this, operational, nonOverlap, micro);
    }

}
