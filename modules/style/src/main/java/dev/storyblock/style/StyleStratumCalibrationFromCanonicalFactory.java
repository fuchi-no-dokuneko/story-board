package dev.storyblock.style;

import java.util.Map;

final class StyleStratumCalibrationFromCanonicalFactory {
    static StyleStratumCalibration fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleStratumCalibration.FIELDS, "style_stratum_calibration");
        StyleStratumCalibration result = new StyleStratumCalibration(
                StyleStratum.fromCanonical(StyleCanonical.object(
                        value.get("stratum"), "style_stratum_calibration.stratum"
                )),
                StyleCanonical.integer(
                        value, "window_count", "style_stratum_calibration"
                ),
                StyleCanonical.objects(
                        value.get("channels"), "style_stratum_calibration.channels"
                ).stream().map(StyleChannelCalibration::fromCanonical).toList()
        );
        StyleCalibrationConfidence supplied = StyleCalibrationConfidence
                .fromCanonicalName(StyleCanonical.string(
                        value, "confidence", "style_stratum_calibration"
                ));
        if (supplied != result.confidence()) {
            throw new IllegalArgumentException(
                    "Style calibration confidence does not match sample count"
            );
        }
        return result;
    }
}
