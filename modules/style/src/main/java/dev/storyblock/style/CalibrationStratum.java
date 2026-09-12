package dev.storyblock.style;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import static dev.storyblock.style.StyleCalibrationEngine.CalibrationGroup;

final class CalibrationStratum {
  static StyleStratumCalibration calibrate(StyleCalibrationEngine self, CalibrationGroup group)  {
    List<StyleWindowFeatures> windows = group.windows();
    EnumMap<StyleFeatureChannel, List<BigDecimal>> distances = new EnumMap<>(
        StyleFeatureChannel.class
    );
    windows.getFirst().featureSet().channels().forEach(vector ->
        distances.put(vector.channel(), new ArrayList<>())
    );
    if (windows.size() > 1) {
      for (int index = 0; index < windows.size(); index++) {
        List<StyleFeatureSet> remainder = new ArrayList<>();
        for (int candidate = 0; candidate < windows.size(); candidate++) {
          if (candidate != index) {
            remainder.add(windows.get(candidate).featureSet());
          }
        }
        StyleDistanceReport report = self.analyzer.compare(
            StyleCalibrationEngineAggregate.aggregate(remainder), windows.get(index).featureSet()
        );
        report.channels().forEach(distance ->
            distances.get(distance.channel()).add(distance.primaryDistance())
        );
      }
    }
    List<StyleChannelCalibration> channels = new ArrayList<>();
    for (StyleFeatureChannel channel : StyleFeatureChannel.values()) {
      if (distances.containsKey(channel)) {
        channels.add(StyleChannelCalibration.fromDistances(
            channel, distances.get(channel)
        ));
      }
    }
    return new StyleStratumCalibration(
        group.stratum(), windows.size(), channels
    );
  }
}
