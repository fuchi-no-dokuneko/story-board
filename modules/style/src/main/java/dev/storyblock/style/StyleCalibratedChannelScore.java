package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record StyleCalibratedChannelScore(
        StyleChannelDistance distance,
        BigDecimal percentile,
        BigDecimal robustZ,
        BigDecimal q95,
        BigDecimal q99,
        boolean aboveQ95,
        boolean aboveQ99
) {
    public StyleCalibratedChannelScore {
        Objects.requireNonNull(distance, "distance");
        percentile = nonNegative(percentile, "percentile");
        robustZ = nonNegative(robustZ, "robustZ");
        q95 = nonNegative(q95, "q95");
        q99 = nonNegative(q99, "q99");
        if (percentile.compareTo(BigDecimal.valueOf(100)) > 0
                || q95.compareTo(q99) > 0
                || aboveQ95 != (distance.primaryDistance().compareTo(q95) > 0)
                || aboveQ99 != (distance.primaryDistance().compareTo(q99) > 0)
                || (aboveQ99 && !aboveQ95)) {
            throw new IllegalArgumentException(
                    "Calibrated style channel score is inconsistent"
            );
        }
    }

    public static StyleCalibratedChannelScore score(
            StyleChannelDistance distance,
            StyleChannelCalibration calibration
    ) {
        if (distance.channel() != calibration.channel()
                || !distance.channelVersion().equals(calibration.channelVersion())
                || distance.primaryMetric() != calibration.primaryMetric()) {
            throw new IllegalArgumentException(
                    "Style distance and calibration channel contracts do not match"
            );
        }
        BigDecimal raw = distance.primaryDistance();
        return new StyleCalibratedChannelScore(
                distance,
                calibration.percentile(raw),
                calibration.robustZ(raw),
                calibration.q95(),
                calibration.q99(),
                raw.compareTo(calibration.q95()) > 0,
                raw.compareTo(calibration.q99()) > 0
        );
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("above_q95", aboveQ95);
        value.put("above_q99", aboveQ99);
        value.put("distance", distance.canonicalValue());
        value.put("percentile", percentile);
        value.put("q95", q95);
        value.put("q99", q99);
        value.put("robust_z", robustZ);
        return CanonicalValues.freezeMap(value, "style_calibrated_channel_score");
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(
                    "Style calibrated " + field + " must be nonnegative"
            );
        }
        return value.stripTrailingZeros();
    }
}
