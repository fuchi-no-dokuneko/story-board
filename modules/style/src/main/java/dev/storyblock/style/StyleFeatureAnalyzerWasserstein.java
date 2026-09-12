package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

final class StyleFeatureAnalyzerWasserstein {
    static double wasserstein(
            Map<String, BigDecimal> left,
            Map<String, BigDecimal> right
    ) {
        Set<String> keys = new java.util.TreeSet<>(Comparator.comparingInt(
                StyleFeatureAnalyzerBucketStart::bucketStart
        ).thenComparing(value -> value));
        keys.addAll(left.keySet());
        keys.addAll(right.keySet());
        double cumulative = 0;
        double result = 0;
        for (String key : keys) {
            cumulative += left.getOrDefault(key, BigDecimal.ZERO).doubleValue()
                    - right.getOrDefault(key, BigDecimal.ZERO).doubleValue();
            result += StrictMath.abs(cumulative);
        }
        return keys.isEmpty() ? 0 : result / keys.size();
    }
}
