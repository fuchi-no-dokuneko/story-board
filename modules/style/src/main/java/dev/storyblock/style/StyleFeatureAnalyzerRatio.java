package dev.storyblock.style;

import java.math.BigDecimal;
import java.math.RoundingMode;
import static dev.storyblock.style.StyleFeatureAnalyzer.SCALE;

final class StyleFeatureAnalyzerRatio {
    static BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).divide(
                BigDecimal.valueOf(denominator), SCALE, RoundingMode.HALF_EVEN
        ).stripTrailingZeros();
    }
}
