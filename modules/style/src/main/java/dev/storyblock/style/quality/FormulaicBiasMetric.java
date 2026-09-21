package dev.storyblock.style.quality;

import java.util.*;

public final class FormulaicBiasMetric implements QualityMetric {
    public String id() { return "formulaic_bias"; }
    public QualityMetricResult measure(QualityWindow window, QualityContract contract, QualityCalibration calibration) {
        int count = window.words().size();
        if (count == 0) return QualityMetricResult.unavailable(id(), QualityMetricResult.Status.INSUFFICIENT_DATA, 0);
        if (calibration.humanSources() == 0 || calibration.aiSources() < 2)
            return QualityMetricResult.unavailable(id(), QualityMetricResult.Status.UNCALIBRATED, count);
        var coverage = new HashMap<Integer, Double>();
        var evidence = new ArrayList<QualityEvidence>();
        QualityPhrases.each(window, contract, phrase -> {
            Double weight = calibration.phraseWeights().get(phrase.key());
            if (weight == null) return;
            for (var token : phrase.words()) coverage.merge(token.wordIndex(), weight, Math::max);
            if (evidence.size() < contract.evidenceLimit()) evidence.add(QualityEvidence.span(window,
                    phrase.words().getFirst().source().start(), phrase.words().getLast().source().end(), "cross-source weight=" + weight));
        });
        return QualityMetricResult.result(id(), coverage.values().stream().mapToDouble(Double::doubleValue).sum() / count, count, evidence);
    }
}
