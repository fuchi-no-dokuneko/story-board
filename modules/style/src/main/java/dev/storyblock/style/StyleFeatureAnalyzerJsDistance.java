package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.Map;
import static dev.storyblock.style.StyleFeatureAnalyzer.Probabilities;

final class StyleFeatureAnalyzerJsDistance {
    static double jsDistance(
            Map<String, BigDecimal> left,
            Map<String, BigDecimal> right,
            double alpha
    ) {
        Probabilities values = StyleFeatureAnalyzerProbabilities.probabilities(left, right, alpha);
        double divergence = 0;
        for (int index = 0; index < values.left().length; index++) {
            double middle = (values.left()[index] + values.right()[index]) / 2.0;
            divergence += 0.5 * values.left()[index]
                    * StrictMath.log(values.left()[index] / middle);
            divergence += 0.5 * values.right()[index]
                    * StrictMath.log(values.right()[index] / middle);
        }
        return StrictMath.sqrt(Math.max(0, divergence));
    }
}
