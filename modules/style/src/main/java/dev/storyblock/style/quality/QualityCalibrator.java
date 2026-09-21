package dev.storyblock.style.quality;

import dev.storyblock.nlp.zh.ChineseTokenizer;
import java.util.*;

public final class QualityCalibrator {
    private QualityCalibrator() { }
    public static QualityCalibration fit(List<QualityCorpusSource> corpus, QualityContract contract) {
        return fit(corpus,contract,new dev.storyblock.nlp.zh.JiebaTokenizer());
    }
    public static QualityCalibration fit(List<QualityCorpusSource> corpus, QualityContract contract, ChineseTokenizer tokenizer) {
        if (corpus.stream().map(QualityCorpusSource::id).distinct().count() != corpus.size())
            throw new IllegalArgumentException("Corpus source ids must be unique");
        var sources = new ArrayList<QualityReferenceFit.Source>();
        for (var input : corpus) {
            var docs = input.texts().stream().map(t -> QualityAnalysis.analyze(t,contract,tokenizer)).toList();
            var windows = docs.stream().flatMap(d -> QualityAnalysis.windows(d,contract).stream()).toList();
            if (windows.isEmpty()) throw new IllegalArgumentException("Corpus source contains no words: " + input.id());
            sources.add(new QualityReferenceFit.Source(input.kind(), docs, windows));
        }
        var humans = sources.stream().filter(s -> s.kind() == QualityCorpusSource.Kind.HUMAN).toList();
        var initial = new QualityCalibration(QualityReferenceFit.boundary(humans,1.0/3),
                QualityReferenceFit.boundary(humans,2.0/3), humans.size(),sources.size()-humans.size(),
                QualityReferenceFit.phrases(sources,contract),QualityReferenceFit.words(humans),Map.of());
        var references = new HashMap<String, List<QualityCalibration.ReferenceScore>>();
        for (var source : humans) {
            var byMetric = new HashMap<String, List<Double>>();
            for (var window : source.windows()) for (var metric : QualityMetrics.ALL) {
                var score = metric.measure(window,contract,initial);
                if (score.value() != null) byMetric.computeIfAbsent(metric.id(),k -> new ArrayList<>()).add(score.value());
            }
            byMetric.forEach((id,scores) -> scores.forEach(value -> references.computeIfAbsent(id,k -> new ArrayList<>())
                    .add(new QualityCalibration.ReferenceScore(value,1.0/scores.size()))));
        }
        return new QualityCalibration(initial.shortMaximum(),initial.mediumMaximum(),initial.humanSources(),initial.aiSources(),
                initial.phraseWeights(),initial.humanWords(),references);
    }
}
