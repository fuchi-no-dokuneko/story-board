package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.style.StyleChannelCalibration.Statistics;

final class StyleChannelCalibrationFromDistancesFactory {
    static StyleChannelCalibration fromDistances(StyleFeatureChannel channel, List<BigDecimal> distances)  {
        List<BigDecimal> sorted = new ArrayList<>(List.copyOf(distances));
        sorted.replaceAll(StyleChannelCalibrationNormalized::normalized);
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
}
