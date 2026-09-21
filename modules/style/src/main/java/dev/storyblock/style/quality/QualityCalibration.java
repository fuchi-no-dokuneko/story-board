package dev.storyblock.style.quality;

import java.util.*;

public record QualityCalibration(Integer shortMaximum, Integer mediumMaximum, int humanSources, int aiSources,
        Map<List<String>, Double> phraseWeights, Map<String, Double> humanWords,
        Map<String, List<ReferenceScore>> referenceScores) {
    public record ReferenceScore(double value, double weight) { }
    public QualityCalibration {
        var phrases = new HashMap<List<String>, Double>();
        phraseWeights.forEach((k,v) -> phrases.put(List.copyOf(k),v)); phraseWeights = Map.copyOf(phrases);
        humanWords = Map.copyOf(humanWords);
        var scores = new HashMap<String, List<ReferenceScore>>();
        referenceScores.forEach((k,v) -> scores.put(k,List.copyOf(v))); referenceScores = Map.copyOf(scores);
    }
    public static QualityCalibration empty() { return new QualityCalibration(null,null,0,0,Map.of(),Map.of(),Map.of()); }
    public Double percentile(String metric, Double value) {
        var scores = referenceScores.get(metric);
        if (value == null || scores == null || scores.isEmpty()) return null;
        double below = 0, total = 0;
        for (var score : scores) {
            total += score.weight();
            if (score.value() < value) below += score.weight();
            else if (score.value() == value) below += score.weight() / 2;
        }
        return 100 * below / total;
    }
}
