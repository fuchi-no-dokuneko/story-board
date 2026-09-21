package dev.storyblock.style.quality;

import java.util.List;

/** Explicit metric registration. */
public final class QualityMetrics {
    private QualityMetrics() { }
    public static final List<QualityMetric> ALL = List.of(new RepeatedPhraseMetric(), new OpeningCollisionMetric(),
            new SkeletonReuseMetric(), new RhythmPredictabilityMetric(), new FormulaicBiasMetric());
    public static List<QualityMetricResult> measure(QualityWindow window, QualityContract contract, QualityCalibration calibration) {
        return ALL.stream().map(metric -> {
            var result = metric.measure(window, contract, calibration);
            return result.percentile(calibration.percentile(metric.id(), result.value()));
        }).toList();
    }
}
