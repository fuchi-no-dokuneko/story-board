package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StyleFeatureAnalyzerTopContributors {
    static List<Map<String, Object>> topContributors(
            StyleFeatureVector target,
            StyleFeatureVector current,
            int limit
    ) {
        Map<String, BigDecimal> targetValues = StyleFeatureAnalyzerContributorValues.contributorValues(target);
        Map<String, BigDecimal> currentValues = StyleFeatureAnalyzerContributorValues.contributorValues(current);
        Set<String> keys = new LinkedHashSet<>(targetValues.keySet());
        keys.addAll(currentValues.keySet());
        return keys.stream()
                .map(key -> Map.<String, Object>of(
                        "absolute_delta",
                        targetValues.getOrDefault(key, BigDecimal.ZERO)
                                .subtract(currentValues.getOrDefault(
                                        key, BigDecimal.ZERO
                                )).abs().stripTrailingZeros(),
                        "feature", key
                ))
                .sorted(Comparator
                        .<Map<String, Object>, BigDecimal>comparing(value ->
                                (BigDecimal) value.get("absolute_delta")
                        ).reversed()
                        .thenComparing(value -> (String) value.get("feature")))
                .limit(limit)
                .toList();
    }
}
