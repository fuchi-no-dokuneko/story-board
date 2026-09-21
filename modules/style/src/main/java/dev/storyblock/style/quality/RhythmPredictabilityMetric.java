package dev.storyblock.style.quality;

import java.util.*;

public final class RhythmPredictabilityMetric implements QualityMetric {
    public String id() { return "rhythm_predictability"; }
    public QualityMetricResult measure(QualityWindow window, QualityContract contract, QualityCalibration calibration) {
        int transitions = Math.max(0, window.sentences().size() - 1);
        if (transitions < contract.minRhythmTransitions()) return QualityMetricResult.unavailable(id(), QualityMetricResult.Status.INSUFFICIENT_DATA, transitions);
        if (calibration.shortMaximum() == null) return QualityMetricResult.unavailable(id(), QualityMetricResult.Status.UNCALIBRATED, transitions);
        int[][] pairs = new int[3][3]; int[] totals = new int[3];
        var evidence = new ArrayList<QualityEvidence>();
        int previous = -1;
        for (var sentence : window.sentences()) {
            int length = sentence.words().size();
            int bin = length <= calibration.shortMaximum() ? 0 : length <= calibration.mediumMaximum() ? 1 : 2;
            if (previous >= 0) {
                pairs[previous][bin]++; totals[previous]++;
                if (evidence.size() < contract.evidenceLimit()) evidence.add(QualityEvidence.span(window,
                        sentence.start(),sentence.end(),"length class " + previous + " -> " + bin + "; words=" + length));
            }
            previous = bin;
        }
        double entropy = 0;
        for (int from = 0; from < 3; from++) for (int to = 0; to < 3; to++) if (pairs[from][to] > 0)
            entropy -= pairs[from][to] / (double)transitions * Math.log(pairs[from][to] / (double)totals[from]) / Math.log(2);
        return QualityMetricResult.result(id(), Math.max(0, 1 - entropy / (Math.log(3) / Math.log(2))), transitions, evidence);
    }
}
