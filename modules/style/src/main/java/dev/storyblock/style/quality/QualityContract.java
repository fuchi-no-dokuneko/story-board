package dev.storyblock.style.quality;

import java.util.*;

/** Independent quality contract: no style-distance or rewrite thresholds. */
public record QualityContract(int windowWords, int strideWords, int minPhraseWords, int maxPhraseWords,
        int openingWords, int minOpeningSentences, int minSkeletonSentences, int minRhythmTransitions,
        int mattrWords, double epsilon, int evidenceLimit, Set<String> functionWords, Set<String> properNames) {
    public QualityContract {
        if (windowWords < 8 || strideWords < 1 || strideWords > windowWords
                || minPhraseWords < 1 || maxPhraseWords < minPhraseWords || maxPhraseWords > 16
                || openingWords < 1 || minOpeningSentences < 2 || minSkeletonSentences < 2
                || minRhythmTransitions < 1 || mattrWords < 2 || !Double.isFinite(epsilon) || epsilon <= 0
                || evidenceLimit < 1 || evidenceLimit > 100)
            throw new IllegalArgumentException("Invalid quality contract");
        functionWords = Set.copyOf(functionWords); properNames = Set.copyOf(properNames);
        if (functionWords.stream().anyMatch(String::isBlank) || properNames.stream().anyMatch(String::isBlank))
            throw new IllegalArgumentException("Empty quality lexicon entry");
    }
    public static QualityContract defaults() {
        return new QualityContract(256, 128, 4, 8, 3, 5, 5, 10, 50, 0.001, 20,
                Set.of("的", "地", "得", "了", "著", "着", "過", "过", "和", "與", "与", "及", "或", "而", "但",
                    "但是", "然而", "因為", "因为", "所以", "如果", "若", "就", "也", "都", "又", "還", "还", "卻", "却",
                    "只", "才", "便", "是", "在", "把", "被", "讓", "让", "對", "对", "從", "从", "到", "為", "为",
                    "不", "沒有", "没有", "並", "并", "且", "雖然", "虽然", "即使", "即便", "於是", "于是", "然後", "然后"), Set.of());
    }
    public QualityContract withNames(Set<String> names) {
        return new QualityContract(windowWords,strideWords,minPhraseWords,maxPhraseWords,openingWords,
                minOpeningSentences,minSkeletonSentences,minRhythmTransitions,mattrWords,epsilon,evidenceLimit,functionWords,names);
    }
}
