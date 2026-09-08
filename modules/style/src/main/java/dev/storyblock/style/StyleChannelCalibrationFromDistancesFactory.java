package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class StyleChannelCalibrationFromDistancesFactory {
    static StyleChannelCalibration fromDistances(StyleFeatureChannel channel, List<BigDecimal> distances)  {
        List<BigDecimal> sorted = new ArrayList<>(List.copyOf(distances));
        sorted.replaceAll(StyleChannelCalibrationNormalized::normalized);
        sorted.sort(BigDecimal::compareTo);
        ChannelStatistics statistics = ChannelStatistics.from(sorted);
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
}
