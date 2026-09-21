package dev.storyblock.style.quality;

import java.util.*;

public final class OpeningCollisionMetric implements QualityMetric {
    public String id() { return "opening_collision"; }
    public QualityMetricResult measure(QualityWindow window, QualityContract contract, QualityCalibration calibration) {
        var groups = new HashMap<List<String>, Integer>();
        var evidence = new ArrayList<QualityEvidence>();
        int total = 0;
        for (var sentence : window.sentences()) {
            if (sentence.words().size() < contract.openingWords()) continue;
            var opening = sentence.words().subList(0, contract.openingWords());
            var key = opening.stream().map(t -> t.source().properName() ? "<PROPER_NAME>" : t.normalized()).toList();
            int count = groups.merge(key, 1, Integer::sum); total++;
            if (count > 1 && evidence.size() < contract.evidenceLimit()) evidence.add(QualityEvidence.span(window,
                    opening.getFirst().source().start(), opening.getLast().source().end(), "repeated opening"));
        }
        if (total < contract.minOpeningSentences()) return QualityMetricResult.unavailable(id(), QualityMetricResult.Status.INSUFFICIENT_DATA, total);
        double pairs = groups.values().stream().mapToDouble(c -> (double)c * (c - 1)).sum();
        return QualityMetricResult.result(id(), pairs / ((double)total * (total - 1)), total, evidence);
    }
}
