package dev.storyblock.style.quality;

import java.util.List;

public record QualitySentence(int index, int start, int end, List<QualityToken> tokens, List<QualityToken> words) {
    public QualitySentence { tokens = List.copyOf(tokens); words = List.copyOf(words); }
}
