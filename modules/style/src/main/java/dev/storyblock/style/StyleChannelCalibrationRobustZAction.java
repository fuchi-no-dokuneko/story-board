package dev.storyblock.style;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class StyleChannelCalibrationRobustZAction {
    static BigDecimal robustZ(StyleChannelCalibration self, BigDecimal distance)  {
        distance = StyleChannelCalibrationNormalized.normalized(distance);
        BigDecimal absolute = distance.subtract(self.median()).abs();
        if (self.mad().signum() == 0) {
            return absolute.signum() == 0
                    ? BigDecimal.ZERO : new BigDecimal("999999");
        }
        return absolute.divide(
                self.mad().multiply(new BigDecimal("1.4826")),
                StyleChannelCalibration.SCALE,
                RoundingMode.HALF_EVEN
        ).stripTrailingZeros();
    }
}
