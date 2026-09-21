package dev.storyblock.style.quality;

import java.util.*;

public final class RepeatedPhraseMetric implements QualityMetric {
    private record First(int sentence, int word) { }
    public String id() { return "repeated_phrase_coverage"; }
    public QualityMetricResult measure(QualityWindow window, QualityContract contract, QualityCalibration calibration) {
        var first = new HashMap<List<String>, First>();
        var crossSentence = new HashSet<List<String>>();
        var covered = new BitSet();
        if (window.words().isEmpty()) return QualityMetricResult.unavailable(id(), QualityMetricResult.Status.INSUFFICIENT_DATA, 0);
        QualityPhrases.each(window, contract, phrase -> {
            var original = first.putIfAbsent(phrase.key(), new First(phrase.sentence(),phrase.words().getFirst().wordIndex()));
            if (original != null && original.sentence() != phrase.sentence()) crossSentence.add(phrase.key());
        });
        QualityPhrases.each(window, contract, phrase -> {
            if (crossSentence.contains(phrase.key()) && first.get(phrase.key()).word() != phrase.words().getFirst().wordIndex())
                phrase.words().forEach(word -> covered.set(word.wordIndex()));
        });
        var evidence = new ArrayList<QualityEvidence>();
        for (var sentence : window.sentences()) {
            int start = -1, end = -1;
            for (var word : sentence.words()) {
                if (covered.get(word.wordIndex())) {
                    if (start < 0) start = word.source().start(); end = word.source().end();
                } else if (start >= 0) {
                    add(window, contract, evidence, start, end); start = -1;
                }
            }
            if (start >= 0) add(window, contract, evidence, start, end);
        }
        return QualityMetricResult.result(id(), covered.cardinality() / (double) window.words().size(), window.words().size(), evidence);
    }
    private void add(QualityWindow w, QualityContract c, List<QualityEvidence> e, int start, int end) {
        if (e.size() < c.evidenceLimit()) e.add(QualityEvidence.span(w,start,end,"repeated across sentences"));
    }
}
