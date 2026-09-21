package dev.storyblock.nlp.zh;

public record ChineseToken(String text, int start, int end, Kind kind, boolean properName) {
    public enum Kind { WORD, PUNCTUATION, SPACE }
    public ChineseToken {
        if (text == null || start < 0 || end <= start || text.length() != end - start || kind == null)
            throw new IllegalArgumentException("Invalid token span");
    }
}
