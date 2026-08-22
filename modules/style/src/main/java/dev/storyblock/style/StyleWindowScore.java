package dev.storyblock.style;

import dev.storyblock.domain.CanonicalValues;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StyleWindowScore(
        StyleWindow window,
        StyleProfileSelection profileSelection,
        StyleDistanceReport distanceReport,
        List<StyleCalibratedChannelScore> channels
) {
    public StyleWindowScore {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(profileSelection, "profileSelection");
        Objects.requireNonNull(distanceReport, "distanceReport");
        channels = List.copyOf(channels);
        if (!profileSelection.requestedStratum().equals(window.requestedStratum())) {
            throw new IllegalArgumentException(
                    "Style profile selection must describe the scored window"
            );
        }
        EnumSet<StyleFeatureChannel> identities = EnumSet.noneOf(
                StyleFeatureChannel.class
        );
        channels.forEach(score -> {
            if (!identities.add(score.distance().channel())) {
                throw new IllegalArgumentException(
                        "Calibrated style window channels must be unique"
                );
            }
        });
        if (profileSelection.calibrationAvailable()) {
            if (!identities.containsAll(StyleFeatureChannel.requiredChannels())) {
                throw new IllegalArgumentException(
                        "Calibrated style window lacks a required channel"
                );
            }
        } else if (!channels.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unavailable calibration cannot produce calibrated channel scores"
            );
        }
    }

    public long independentAboveQ95() {
        return channels.stream().filter(score ->
                score.distance().independentGateEvidence() && score.aboveQ95()
        ).count();
    }

    public long independentAboveQ99() {
        return channels.stream().filter(score ->
                score.distance().independentGateEvidence() && score.aboveQ99()
        ).count();
    }

    public boolean surfaceOnlyAboveQ95() {
        List<StyleCalibratedChannelScore> breached = channels.stream()
                .filter(score -> score.distance().independentGateEvidence()
                        && score.aboveQ95())
                .toList();
        return breached.size() == 1
                && breached.getFirst().distance().channel() == StyleFeatureChannel.SURFACE;
    }

    public boolean hasAnyAboveQ95() {
        return channels.stream().anyMatch(StyleCalibratedChannelScore::aboveQ95);
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("channels", channels.stream()
                .map(StyleCalibratedChannelScore::canonicalValue).toList());
        value.put("distance_report", distanceReport.canonicalValue());
        value.put("profile_selection", profileSelection.canonicalValue());
        value.put("window", window.canonicalValue());
        return CanonicalValues.freezeMap(value, "style_window_score");
    }
}
