package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record StyleProfileVersionContent(
    StyleProfileScope scope,
    List<StyleCorpusSource> corpusSources,
    StyleFeatureSet featureSet,
    StyleWindowConfiguration windowConfiguration,
    Map<String, Object> calibrationStatistics
) {
  static final Set<String> FIELDS = Set.of(
      "scope", "corpus_sources", "feature_set", "window_configuration",
      "calibration_statistics"
  );

  public StyleProfileVersionContent {
    java.util.Objects.requireNonNull(scope, "scope");
    corpusSources = List.copyOf(corpusSources);
    if (corpusSources.isEmpty() || corpusSources.size() > 1_000) {
      throw new IllegalArgumentException(
          "Style profile version requires 1 to 1000 corpus sources"
      );
    }
    if (new HashSet<>(corpusSources.stream()
        .map(StyleCorpusSource::sourceId).toList()).size() != corpusSources.size()) {
      throw new IllegalArgumentException("Style corpus source IDs must be unique");
    }
    java.util.Objects.requireNonNull(featureSet, "featureSet");
    if (corpusSources.stream().noneMatch(source ->
        source.contentHash().equals(featureSet.sourceHash()))) {
      throw new IllegalArgumentException(
          "A style corpus source hash must bind the extracted feature source"
      );
    }
    java.util.Objects.requireNonNull(windowConfiguration, "windowConfiguration");
    calibrationStatistics = CanonicalValues.freezeMap(
        calibrationStatistics, "style_profile_version.calibration_statistics"
    );
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

  public static StyleProfileVersionContent fromCanonical(Map<String, Object> value) {
    return StyleProfileVersionContentFromCanonicalFactory.fromCanonical(value);
  }

  public boolean containsGeneratedText() {
    return corpusSources.stream().anyMatch(source ->
        source.kind().requiresExplicitGeneratedPromotion()
    );
  }

  public Optional<StyleCalibrationProfile> calibrationProfile() {
    return calibrationStatistics.isEmpty()
        ? Optional.empty()
        : Optional.of(StyleCalibrationProfile.fromCanonical(
            calibrationStatistics
        ));
  }

  public boolean hasGateCalibration() {
    return calibrationProfile().map(StyleCalibrationProfile::hasCalibratedStratum)
        .orElse(false);
  }

  public Map<String, Object> canonicalValue() {
    return StyleProfileVersionContentCanonicalValueAction.canonicalValue(this);
  }
}
