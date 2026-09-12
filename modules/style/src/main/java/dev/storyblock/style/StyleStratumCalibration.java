package dev.storyblock.style;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record StyleStratumCalibration(
        StyleStratum stratum,
        int windowCount,
        List<StyleChannelCalibration> channels
) {
    static final Set<String> FIELDS = Set.of(
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
        return StyleStratumCalibrationFromCanonicalFactory.fromCanonical(value);
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
        return StyleStratumCalibrationCanonicalValueAction.canonicalValue(this);
    }
}
