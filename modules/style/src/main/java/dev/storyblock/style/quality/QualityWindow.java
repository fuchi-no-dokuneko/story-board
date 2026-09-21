package dev.storyblock.style.quality;

import java.util.List;

public record QualityWindow(String text, int start, int end, List<QualitySentence> sentences, List<QualityToken> words) {
    public QualityWindow { sentences = List.copyOf(sentences); words = List.copyOf(words); }
    public static QualityWindow of(String text, List<QualitySentence> sentences) {
        return new QualityWindow(text, sentences.isEmpty() ? 0 : sentences.getFirst().start(),
                sentences.isEmpty() ? 0 : sentences.getLast().end(), sentences,
                sentences.stream().flatMap(s -> s.words().stream()).toList());
    }
}
