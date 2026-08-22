package dev.storyblock.style;

import java.util.List;
import java.util.Objects;

public final class StyleWindowScorer {
    private final StyleStratumSelector selector;

    public StyleWindowScorer() {
        this(new StyleStratumSelector());
    }

    StyleWindowScorer(StyleStratumSelector selector) {
        this.selector = Objects.requireNonNull(selector, "selector");
    }

    public StyleWindowScore score(
            StyleWindow window,
            StyleDistanceReport report,
            StyleCalibrationProfile calibration
    ) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(calibration, "calibration");
        if (!report.contractHash().equals(calibration.contractHash())) {
            throw new IllegalArgumentException(
                    "Style distance report and calibration contracts do not match"
            );
        }
        StyleProfileSelection selection = selector.select(
                window.requestedStratum(), calibration
        );
        List<StyleCalibratedChannelScore> channels = calibration
                .find(selection.selectedStratum())
                .map(stratum -> report.channels().stream()
                        .map(distance -> StyleCalibratedChannelScore.score(
                                distance, stratum.require(distance.channel())
                        )).toList())
                .orElse(List.of());
        return new StyleWindowScore(window, selection, report, channels);
    }
}
