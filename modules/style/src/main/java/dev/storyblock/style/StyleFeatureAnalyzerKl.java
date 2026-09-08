package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.Map;
import static dev.storyblock.style.StyleFeatureAnalyzer.Probabilities;

final class StyleFeatureAnalyzerKl {
    static double kl(
            Map<String, BigDecimal> left,
            Map<String, BigDecimal> right,
            double alpha
    ) {
        Probabilities values = StyleFeatureAnalyzerProbabilities.probabilities(left, right, alpha);
        double result = 0;
        for (int index = 0; index < values.left().length; index++) {
            result += values.left()[index]
                    * StrictMath.log(values.left()[index] / values.right()[index]);
        }
        return Math.max(0, result);
    }
}
