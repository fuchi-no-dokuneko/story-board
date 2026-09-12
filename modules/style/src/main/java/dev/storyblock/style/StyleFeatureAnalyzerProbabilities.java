package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import static dev.storyblock.style.StyleFeatureAnalyzer.Probabilities;

final class StyleFeatureAnalyzerProbabilities {
    static Probabilities probabilities(
            Map<String, BigDecimal> left,
            Map<String, BigDecimal> right,
            double alpha
    ) {
        Set<String> keys = new java.util.TreeSet<>();
        keys.addAll(left.keySet());
        keys.addAll(right.keySet());
        if (keys.isEmpty()) {
            keys.add("OTHER");
        }
        double leftTotal = left.values().stream().mapToDouble(BigDecimal::doubleValue).sum()
                + alpha * keys.size();
        double rightTotal = right.values().stream().mapToDouble(BigDecimal::doubleValue).sum()
                + alpha * keys.size();
        double[] normalizedLeft = new double[keys.size()];
        double[] normalizedRight = new double[keys.size()];
        int index = 0;
        for (String key : keys) {
            normalizedLeft[index] = (left.getOrDefault(key, BigDecimal.ZERO).doubleValue()
                    + alpha) / leftTotal;
            normalizedRight[index] = (right.getOrDefault(key, BigDecimal.ZERO).doubleValue()
                    + alpha) / rightTotal;
            index++;
        }
        return new Probabilities(normalizedLeft, normalizedRight);
    }
}
