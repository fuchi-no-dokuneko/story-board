package dev.storyblock.style.quality;

import dev.storyblock.nlp.zh.ChineseToken;

public record QualityToken(ChineseToken source, String normalized, boolean functionWord, int wordIndex) {
    public boolean word() { return source.kind() == ChineseToken.Kind.WORD; }
}
