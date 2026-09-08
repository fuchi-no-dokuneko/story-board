package dev.storyblock.style;

import java.util.Map;

final class StyleCalibrationProfileFromCanonicalFactory {
    static StyleCalibrationProfile fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleCalibrationProfile.FIELDS, "style_calibration_profile");
        return new StyleCalibrationProfile(
                StyleCanonical.string(
                        value, "calibration_schema_version", "style_calibration_profile"
                ),
                StyleCanonical.string(
                        value, "target_corpus_hash", "style_calibration_profile"
                ),
                StyleCanonical.string(
                        value, "contract_hash", "style_calibration_profile"
                ),
                StyleCanonical.string(
                        value, "window_configuration_hash", "style_calibration_profile"
                ),
                StyleCanonical.objects(
                        value.get("strata"), "style_calibration_profile.strata"
                ).stream().map(StyleStratumCalibration::fromCanonical).toList()
        );
    }
}
