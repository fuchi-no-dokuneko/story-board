package dev.storyblock.rewrite.policy;

import dev.storyblock.style.StyleAnalysisWindowFinding;
import dev.storyblock.style.StyleAnomalyDecision;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class RewriteEligibilityPolicyDecision {
    static StyleAnomalyDecision decision(
            StyleAnalysisWindowFinding finding
    ) {
        Map<String, Object> payload = finding.payload();
        if (!payload.keySet().equals(Set.of("decision"))
                || !(payload.get("decision") instanceof Map<?, ?> raw)) {
            throw new RewriteEligibilityException(
                    "Rewrite finding lacks an exact anomaly decision payload"
            );
        }
        Map<String, Object> value = new HashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new RewriteEligibilityException(
                        "Rewrite anomaly decision contains a non-string field"
                );
            }
            value.put(key, entry.getValue());
        }
        try {
            return StyleAnomalyDecision.fromCanonical(value);
        } catch (IllegalArgumentException failure) {
            throw new RewriteEligibilityException(
                    "Rewrite anomaly decision is not eligible"
            );
        }
    }
}
