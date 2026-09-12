package dev.storyblock.style;

import java.util.Map;
import static dev.storyblock.style.StyleProfileVersionContent.*;

final class StyleProfileVersionContentValidation {
  static void validate(StyleFeatureSet featureSet, StyleWindowConfiguration windowConfiguration, Map<String, Object> calibrationStatistics) {
    if (!calibrationStatistics.isEmpty()) {
          StyleCalibrationProfile calibration = StyleCalibrationProfile.fromCanonical(
              calibrationStatistics
          );
          if (!calibration.targetCorpusHash().equals(featureSet.sourceHash())
              || !calibration.contractHash().equals(
                  featureSet.contract().contractHash()
              )
              || !calibration.windowConfigurationHash().equals(
                  windowConfiguration.configurationHash()
              )) {
            throw new IllegalArgumentException(
                "Style calibration profile does not match immutable version content"
            );
          }
          calibrationStatistics = calibration.canonicalValue();
        }
  }
}
