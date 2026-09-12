package dev.storyblock.rewrite.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static dev.storyblock.rewrite.policy.RewriteRiskEvaluator.FactKey;

final class RewriteRiskEvaluatorIndex {
    static Map<FactKey, Integer> index(List<RewriteProtectedFact> values) {
        Map<FactKey, Integer> result = new HashMap<>();
        values.forEach(value -> result.put(
                new FactKey(value.kind(), value.valueHash()), value.count()
        ));
        return result;
    }
}
