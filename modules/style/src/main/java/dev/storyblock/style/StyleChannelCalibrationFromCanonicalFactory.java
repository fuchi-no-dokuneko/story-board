package dev.storyblock.style;

import java.util.Map;

final class StyleChannelCalibrationFromCanonicalFactory {
    static StyleChannelCalibration fromCanonical(Map<String, Object> value)  {
        StyleCanonical.requireKeys(value, StyleChannelCalibration.FIELDS, "style_channel_calibration");
        StyleFeatureChannel channel = StyleFeatureChannel.fromCanonicalName(
                StyleCanonical.string(value, "channel", "style_channel_calibration")
        );
        return new StyleChannelCalibration(
                channel,
                StyleCanonical.string(
                        value, "channel_version", "style_channel_calibration"
                ),
                StyleDistanceMetric.fromCanonicalName(StyleCanonical.string(
                        value, "primary_metric", "style_channel_calibration"
                )),
                StyleCanonical.decimalList(
                        value.get("reference_distances"),
                        "style_channel_calibration.reference_distances"
                ),
                StyleCanonical.decimal(value, "median", "style_channel_calibration"),
                StyleCanonical.decimal(value, "mad", "style_channel_calibration"),
                StyleCanonical.decimal(value, "q95", "style_channel_calibration"),
                StyleCanonical.decimal(value, "q99", "style_channel_calibration")
        );
    }
}
