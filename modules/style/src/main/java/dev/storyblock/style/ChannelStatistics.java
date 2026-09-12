package dev.storyblock.style;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import static dev.storyblock.style.StyleChannelCalibration.SCALE;
record ChannelStatistics(
    BigDecimal median,
    BigDecimal mad,
    BigDecimal q95,
    BigDecimal q99
) {
  static ChannelStatistics from(List<BigDecimal> sorted) {
    if (sorted.isEmpty()) {
      return new ChannelStatistics(
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
    return new ChannelStatistics(
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
