package dev.storyblock.style.quality;

import java.util.*;

final class QualityReferenceFit {
    record Source(QualityCorpusSource.Kind kind, List<QualityWindow> documents, List<QualityWindow> windows) { }
    static Map<List<String>, Double> phrases(List<Source> sources, QualityContract contract) {
        var human = new HashMap<List<String>, Double>(); var ai = new HashMap<List<String>, Double>();
        var support = new HashMap<List<String>, Integer>();
        long humans = sources.stream().filter(s -> s.kind() == QualityCorpusSource.Kind.HUMAN).count();
        long ais = sources.size() - humans;
        if (humans == 0 || ais < 2) return Map.of();
        for (var source : sources) {
            var counts = new HashMap<List<String>, Integer>();
            for (var window : source.windows()) QualityPhrases.distinct(window, contract).forEach(p -> counts.merge(p,1,Integer::sum));
            boolean isHuman = source.kind() == QualityCorpusSource.Kind.HUMAN;
            counts.forEach((phrase,count) -> {
                (isHuman ? human : ai).merge(phrase, count / (double)source.windows().size() / (isHuman ? humans : ais), Double::sum);
                if (!isHuman) support.merge(phrase,1,Integer::sum);
            });
        }
        var weights = new HashMap<List<String>, Double>();
        ai.forEach((phrase,rate) -> {
            double weight = Math.log((rate + contract.epsilon()) / (human.getOrDefault(phrase,0.0) + contract.epsilon()));
            if (support.get(phrase) >= 2 && weight > 0) weights.put(phrase, weight);
        });
        return weights;
    }
    static Map<String, Double> words(List<Source> humans) {
        var result = new HashMap<String, Double>();
        for (var source : humans) {
            var counts = new HashMap<String, Integer>(); int total = 0;
            for (var document : source.documents()) for (var word : document.words()) { counts.merge(word.normalized(),1,Integer::sum); total++; }
            final int size = total;
            counts.forEach((word,count) -> result.merge(word, count / (double)size / humans.size(), Double::sum));
        }
        return result;
    }
    static Integer boundary(List<Source> humans, double quantile) {
        var histogram = new TreeMap<Integer, Double>();
        for (var source : humans) {
            var sentences = source.documents().stream().flatMap(d -> d.sentences().stream()).toList();
            for (var sentence : sentences) histogram.merge(sentence.words().size(), 1.0 / sentences.size() / humans.size(), Double::sum);
        }
        double mass = 0;
        for (var entry : histogram.entrySet()) { mass += entry.getValue(); if (mass + 1e-12 >= quantile) return entry.getKey(); }
        return histogram.isEmpty() ? null : histogram.lastKey();
    }
}
