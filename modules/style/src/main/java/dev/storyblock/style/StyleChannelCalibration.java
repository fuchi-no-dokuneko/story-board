package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record StyleChannelCalibration(
        StyleFeatureChannel channel,
        String channelVersion,
        StyleDistanceMetric primaryMetric,
        List<BigDecimal> referenceDistances,
        BigDecimal median,
        BigDecimal mad,
        BigDecimal q95,
        BigDecimal q99
) {
    private static final int SCALE = 12;
    private static final Set<String> FIELDS = Set.of(
            "channel", "channel_version", "primary_metric", "reference_distances",
            "median", "mad", "q95", "q99"
    );

    public StyleChannelCalibration {
        Objects.requireNonNull(channel, "channel");
        if (!channel.featureVersion().equals(channelVersion)
                || channel.primaryMetric() != primaryMetric) {
            throw new IllegalArgumentException(
                    "Style channel calibration contract is inconsistent"
            );
        }
        List<BigDecimal> sorted = new ArrayList<>(List.copyOf(referenceDistances));
        if (sorted.size() > 1_000 || sorted.stream().anyMatch(value ->
                value == null || value.signum() < 0
        )) {
            throw new IllegalArgumentException(
                    "Style calibration distances must be nonnegative and bounded"
            );
        }
        sorted.replaceAll(StyleChannelCalibration::normalized);
        sorted.sort(BigDecimal::compareTo);
        referenceDistances = List.copyOf(sorted);
        Statistics calculated = Statistics.from(referenceDistances);
        if (!calculated.median().equals(normalized(median))
                || !calculated.mad().equals(normalized(mad))
                || !calculated.q95().equals(normalized(q95))
                || !calculated.q99().equals(normalized(q99))) {
            throw new IllegalArgumentException(
                    "Style calibration summaries do not match reference distances"
            );
        }
        median = calculated.median();
        mad = calculated.mad();
        q95 = calculated.q95();
        q99 = calculated.q99();
    }

    public static StyleChannelCalibration fromDistances(
            StyleFeatureChannel channel,
            List<BigDecimal> distances
    ) {
        List<BigDecimal> sorted = new ArrayList<>(List.copyOf(distances));
        sorted.replaceAll(StyleChannelCalibration::normalized);
        sorted.sort(BigDecimal::compareTo);
        Statistics statistics = Statistics.from(sorted);
        return new StyleChannelCalibration(
                channel,
                channel.featureVersion(),
                channel.primaryMetric(),
                sorted,
                statistics.median(),
                statistics.mad(),
                statistics.q95(),
                statistics.q99()
        );
    }

    public static StyleChannelCalibration fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_channel_calibration");
        StyleFeatureChannel channel = StyleFeatureChannel.fromCanonicalName(
                StyleCanonical.string(value, "channel", "style_channel_calibration")
        );
        return new StyleChannelCalibration(
                channel,
                StyleCanonical.string(
                        value, "channel_version", "style_channel_calibration"
                ),
                StyleDistanceMetric.fromCanonicalName(StyleCanonical.string(
                        value, "primary_metric", "style_channel_calibration"
                )),
                StyleCanonical.decimalList(
                        value.get("reference_distances"),
                        "style_channel_calibration.reference_distances"
                ),
                StyleCanonical.decimal(value, "median", "style_channel_calibration"),
                StyleCanonical.decimal(value, "mad", "style_channel_calibration"),
                StyleCanonical.decimal(value, "q95", "style_channel_calibration"),
                StyleCanonical.decimal(value, "q99", "style_channel_calibration")
        );
    }

    public BigDecimal percentile(BigDecimal distance) {
        BigDecimal normalizedDistance = normalized(distance);
        if (referenceDistances.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long atOrBelow = referenceDistances.stream()
                .filter(reference -> reference.compareTo(normalizedDistance) <= 0)
                .count();
        return BigDecimal.valueOf(atOrBelow)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(referenceDistances.size()),
                        SCALE,
                        RoundingMode.HALF_EVEN
                ).stripTrailingZeros();
    }

    public BigDecimal robustZ(BigDecimal distance) {
        distance = normalized(distance);
        BigDecimal absolute = distance.subtract(median).abs();
        if (mad.signum() == 0) {
            return absolute.signum() == 0
                    ? BigDecimal.ZERO : new BigDecimal("999999");
        }
        return absolute.divide(
                mad.multiply(new BigDecimal("1.4826")),
                SCALE,
                RoundingMode.HALF_EVEN
        ).stripTrailingZeros();
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channel", channel.canonicalName());
        value.put("channel_version", channelVersion);
        value.put("mad", mad);
        value.put("median", median);
        value.put("primary_metric", primaryMetric.canonicalName());
        value.put("q95", q95);
        value.put("q99", q99);
        value.put("reference_distances", referenceDistances);
        return CanonicalValues.freezeMap(value, "style_channel_calibration");
    }

    private static BigDecimal normalized(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Style calibration value is invalid");
        }
        return value.stripTrailingZeros();
    }

    private record Statistics(
            BigDecimal median,
            BigDecimal mad,
            BigDecimal q95,
            BigDecimal q99
    ) {
        static Statistics from(List<BigDecimal> sorted) {
            if (sorted.isEmpty()) {
                return new Statistics(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );
            }
            BigDecimal median = median(sorted);
            List<BigDecimal> deviations = sorted.stream()
                    .map(value -> value.subtract(median).abs())
                    .sorted()
                    .toList();
            return new Statistics(
                    normalized(median),
                    normalized(median(deviations)),
                    normalized(quantile(sorted, 95)),
                    normalized(quantile(sorted, 99))
            );
        }

        private static BigDecimal median(List<BigDecimal> sorted) {
            int middle = sorted.size() / 2;
            if (sorted.size() % 2 == 1) {
                return sorted.get(middle);
            }
            return sorted.get(middle - 1).add(sorted.get(middle))
                    .divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_EVEN);
        }

        private static BigDecimal quantile(List<BigDecimal> sorted, int percentile) {
            int rank = (int) StrictMath.ceil(percentile / 100.0 * sorted.size());
            return sorted.get(Math.max(0, rank - 1));
        }
    }
}
