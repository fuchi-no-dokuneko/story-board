package dev.storyblock.style;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import static dev.storyblock.style.StyleFeatureAnalyzer.SCALE;

final class StyleFeatureAnalyzerMean {
    static BigDecimal mean(List<Integer> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long sum = values.stream().mapToLong(Integer::longValue).sum();
        return BigDecimal.valueOf(sum).divide(
                BigDecimal.valueOf(values.size()), SCALE, RoundingMode.HALF_EVEN
        ).stripTrailingZeros();
    }
}
