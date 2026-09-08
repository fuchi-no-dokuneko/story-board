package dev.storyblock.style;

import java.math.BigDecimal;

final class StyleChannelCalibrationNormalized {
    static BigDecimal normalized(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Style calibration value is invalid");
        }
        return value.stripTrailingZeros();
    }
}
