package dev.storyblock.style;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
  static final int SCALE = 12;
  static final Set<String> FIELDS = Set.of(
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
    sorted.replaceAll(StyleChannelCalibrationNormalized::normalized);
    sorted.sort(BigDecimal::compareTo);
    referenceDistances = List.copyOf(sorted);
    Statistics calculated = Statistics.from(referenceDistances);
    if (!calculated.median().equals(StyleChannelCalibrationNormalized.normalized(median))
        || !calculated.mad().equals(StyleChannelCalibrationNormalized.normalized(mad))
        || !calculated.q95().equals(StyleChannelCalibrationNormalized.normalized(q95))
        || !calculated.q99().equals(StyleChannelCalibrationNormalized.normalized(q99))) {
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
    return StyleChannelCalibrationFromDistancesFactory.fromDistances(channel, distances);
  }

  public static StyleChannelCalibration fromCanonical(Map<String, Object> value) {
    return StyleChannelCalibrationFromCanonicalFactory.fromCanonical(value);
  }

  public BigDecimal percentile(BigDecimal distance) {
    return StyleChannelCalibrationPercentileAction.percentile(this, distance);
  }

  public BigDecimal robustZ(BigDecimal distance) {
    return StyleChannelCalibrationRobustZAction.robustZ(this, distance);
  }

  public Map<String, Object> canonicalValue() {
    return StyleChannelCalibrationCanonicalValueAction.canonicalValue(this);
  }

  record Statistics(
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
          StyleChannelCalibrationNormalized.normalized(median),
          StyleChannelCalibrationNormalized.normalized(median(deviations)),
          StyleChannelCalibrationNormalized.normalized(quantile(sorted, 95)),
          StyleChannelCalibrationNormalized.normalized(quantile(sorted, 99))
      );
    }

    static BigDecimal median(List<BigDecimal> sorted) {
      int middle = sorted.size() / 2;
      if (sorted.size() % 2 == 1) {
        return sorted.get(middle);
      }
      return sorted.get(middle - 1).add(sorted.get(middle))
          .divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_EVEN);
    }

    static BigDecimal quantile(List<BigDecimal> sorted, int percentile) {
      int rank = (int) StrictMath.ceil(percentile / 100.0 * sorted.size());
      return sorted.get(Math.max(0, rank - 1));
    }
  }
}
