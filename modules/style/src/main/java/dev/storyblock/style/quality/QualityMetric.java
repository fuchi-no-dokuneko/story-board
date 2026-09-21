package dev.storyblock.style.quality;

/** Pure computation: no database, HTTP, mutable dictionary or scheduler. */
public interface QualityMetric {
    String id();
    QualityMetricResult measure(QualityWindow window, QualityContract contract, QualityCalibration calibration);
}
