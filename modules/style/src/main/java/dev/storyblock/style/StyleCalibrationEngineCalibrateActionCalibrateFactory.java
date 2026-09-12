package dev.storyblock.style;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import static dev.storyblock.style.StyleCalibrationEngine.CalibrationGroup;

final class StyleCalibrationEngineCalibrateActionCalibrateFactory {
  static StyleCalibrationProfile calibrate(StyleCalibrationEngine self, String targetCorpusHash, StyleWindowConfiguration configuration, List<StyleWindowFeatures> windows)  {
    if (targetCorpusHash == null || !StyleCalibrationEngine.HASH.matcher(targetCorpusHash).matches()) {
      throw new IllegalArgumentException(
          "Style calibration target corpus hash is invalid"
      );
    }
    Objects.requireNonNull(configuration, "configuration");
    List<StyleWindowFeatures> eligible = List.copyOf(windows).stream()
        .filter(candidate -> candidate.window().primaryDecisionEligible())
        .sorted(Comparator.comparing(candidate -> candidate.window().windowId()))
        .toList();
    if (eligible.isEmpty()) {
      throw new IllegalArgumentException(
          "Style calibration requires at least one full operational window"
      );
    }
    if (eligible.stream().map(candidate -> candidate.window().windowId())
        .distinct().count() != eligible.size()) {
      throw new IllegalArgumentException(
          "Style calibration windows must have unique identities"
      );
    }
    String contractHash = eligible.getFirst().featureSet().contract().contractHash();
    for (StyleWindowFeatures candidate : eligible) {
      if (!contractHash.equals(
          candidate.featureSet().contract().contractHash()
      )) {
        throw new IllegalArgumentException(
            "Style calibration windows must use one feature contract"
        );
      }
    }

    Map<String, CalibrationGroup> groups = new TreeMap<>();
    for (StyleWindowFeatures candidate : eligible) {
      StyleCalibrationEngineAdd.add(groups, candidate.window().requestedStratum(), candidate);
      if (candidate.window().requestedStratum().speakerSpecific()) {
        StyleCalibrationEngineAdd.add(groups, StyleStratum.dialogue(), candidate);
      }
    }
    List<StyleStratumCalibration> strata = groups.values().stream()
        .map(self::calibrate)
        .toList();
    return new StyleCalibrationProfile(
        StyleModule.CALIBRATION_SCHEMA_VERSION,
        targetCorpusHash,
        contractHash,
        configuration.configurationHash(),
        strata
    );
  }

}
