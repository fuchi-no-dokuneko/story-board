package dev.storyblock.style;

import java.math.BigDecimal;
import java.math.RoundingMode;
import static dev.storyblock.style.StyleFeatureAnalyzer.SCALE;

final class StyleFeatureAnalyzerDecimal {
    static BigDecimal decimal(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Style distance is not finite");
        }
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_EVEN)
                .stripTrailingZeros();
    }
}
