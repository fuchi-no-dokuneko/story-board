package dev.storyblock.style.quality;

import java.util.List;

public record QualityIndexReport(QualityContract contract, int words, int sentences,
        CalibrationSummary calibration, List<QualityMetricResult> metrics, QualityStatistics statistics,
        List<WindowReport> windows) {
    public record CalibrationSummary(int humanSources, int aiSources, Integer shortMaximum, Integer mediumMaximum) { }
    public record WindowReport(int start, int end, int words, int sentences,
            List<QualityMetricResult> metrics, QualityStatistics statistics) {
        public WindowReport { metrics = List.copyOf(metrics); }
    }
    public QualityIndexReport { metrics = List.copyOf(metrics); windows = List.copyOf(windows); }
}
