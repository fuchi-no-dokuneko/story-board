package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class StyleFeatureAnalyzerL1 {
    static double l1(StyleFeatureVector left, StyleFeatureVector right) {
        Map<String, BigDecimal> leftValues = new LinkedHashMap<>(left.distribution());
        left.measurements().forEach((key, value) -> leftValues.put("measure:" + key, value));
        Map<String, BigDecimal> rightValues = new LinkedHashMap<>(right.distribution());
        right.measurements().forEach((key, value) -> rightValues.put("measure:" + key, value));
        Set<String> keys = new LinkedHashSet<>(leftValues.keySet());
        keys.addAll(rightValues.keySet());
        return keys.stream().mapToDouble(key -> StrictMath.abs(
                leftValues.getOrDefault(key, BigDecimal.ZERO).doubleValue()
                        - rightValues.getOrDefault(key, BigDecimal.ZERO).doubleValue()
        )).average().orElse(0);
    }
}
