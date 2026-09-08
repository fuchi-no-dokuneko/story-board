package dev.storyblock.rewrite.policy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import static dev.storyblock.rewrite.policy.RewriteRiskEvaluator.FactKey;

final class RewriteRiskEvaluatorCompare {
    static List<RewriteFactDifference> compare(
            RewriteProtectedFactSnapshot before,
            RewriteProtectedFactSnapshot after
    ) {
        Map<FactKey, Integer> source = RewriteRiskEvaluatorIndex.index(before.facts());
        Map<FactKey, Integer> candidate = RewriteRiskEvaluatorIndex.index(after.facts());
        LinkedHashSet<FactKey> keys = new LinkedHashSet<>(source.keySet());
        keys.addAll(candidate.keySet());
        return keys.stream()
                .filter(key -> !source.getOrDefault(key, 0).equals(
                        candidate.getOrDefault(key, 0)
                ))
                .map(key -> new RewriteFactDifference(
                        before.blockId(),
                        key.kind(),
                        key.valueHash(),
                        source.getOrDefault(key, 0),
                        candidate.getOrDefault(key, 0),
                        key.kind().changedDisposition()
                ))
                .toList();
    }
}
