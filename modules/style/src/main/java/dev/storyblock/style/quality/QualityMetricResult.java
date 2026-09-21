package dev.storyblock.style.quality;

import java.util.List;

public record QualityMetricResult(String metric, Status status, Double value, int validSamples,
        Double referencePercentile, List<QualityEvidence> evidence) {
    public enum Status { OK, INSUFFICIENT_DATA, UNCALIBRATED }
    public QualityMetricResult { evidence = List.copyOf(evidence); }
    static QualityMetricResult result(String id, double value, int count, List<QualityEvidence> evidence) {
        return new QualityMetricResult(id, Status.OK, value, count, null, evidence);
    }
    static QualityMetricResult unavailable(String id, Status status, int count) {
        return new QualityMetricResult(id, status, null, count, null, List.of());
    }
    QualityMetricResult percentile(Double percentile) {
        return new QualityMetricResult(metric, status, value, validSamples, percentile, evidence);
    }
}
