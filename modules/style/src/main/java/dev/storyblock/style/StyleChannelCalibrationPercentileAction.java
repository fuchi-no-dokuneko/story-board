package dev.storyblock.style;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class StyleChannelCalibrationPercentileAction {
    static BigDecimal percentile(StyleChannelCalibration self, BigDecimal distance)  {
        BigDecimal normalizedDistance = StyleChannelCalibrationNormalized.normalized(distance);
        if (self.referenceDistances().isEmpty()) {
            return BigDecimal.ZERO;
        }
        long atOrBelow = self.referenceDistances().stream()
                .filter(reference -> reference.compareTo(normalizedDistance) <= 0)
                .count();
        return BigDecimal.valueOf(atOrBelow)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(self.referenceDistances().size()),
                        StyleChannelCalibration.SCALE,
                        RoundingMode.HALF_EVEN
                ).stripTrailingZeros();
    }
}
