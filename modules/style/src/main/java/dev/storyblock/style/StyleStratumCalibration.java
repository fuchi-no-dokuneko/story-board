package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record StyleStratumCalibration(
        StyleStratum stratum,
        int windowCount,
        List<StyleChannelCalibration> channels
) {
    private static final Set<String> FIELDS = Set.of(
            "stratum", "window_count", "confidence", "channels"
    );

    public StyleStratumCalibration {
        Objects.requireNonNull(stratum, "stratum");
        if (windowCount < 1 || windowCount > 1_000) {
            throw new IllegalArgumentException(
                    "Style stratum calibration window count is invalid"
            );
        }
        channels = List.copyOf(channels);
        EnumSet<StyleFeatureChannel> identities = EnumSet.noneOf(
                StyleFeatureChannel.class
        );
        for (StyleChannelCalibration channel : channels) {
            if (!identities.add(channel.channel())) {
                throw new IllegalArgumentException(
                        "Style stratum calibration channels must be unique"
                );
            }
            int expectedReferences = windowCount < 2 ? 0 : windowCount;
            if (channel.referenceDistances().size() != expectedReferences) {
                throw new IllegalArgumentException(
                        "Leave-one-out reference count must match calibration windows"
                );
            }
        }
        if (!identities.containsAll(StyleFeatureChannel.requiredChannels())) {
            throw new IllegalArgumentException(
                    "Style stratum calibration lacks a required channel"
            );
        }
    }

    public static StyleStratumCalibration fromCanonical(Map<String, Object> value) {
        StyleCanonical.requireKeys(value, FIELDS, "style_stratum_calibration");
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

    public StyleCalibrationConfidence confidence() {
        return windowCount >= StyleModule.MIN_CALIBRATION_WINDOWS
                ? StyleCalibrationConfidence.CALIBRATED
                : StyleCalibrationConfidence.LOW_CONFIDENCE;
    }

    public StyleChannelCalibration require(StyleFeatureChannel channel) {
        return channels.stream()
                .filter(candidate -> candidate.channel() == channel)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Style stratum calibration lacks " + channel.canonicalName()
                ));
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channels", channels.stream()
                .map(StyleChannelCalibration::canonicalValue).toList());
        value.put("confidence", confidence().canonicalName());
        value.put("stratum", stratum.canonicalValue());
        value.put("window_count", windowCount);
        return CanonicalValues.freezeMap(value, "style_stratum_calibration");
    }
}
