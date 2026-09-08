package dev.storyblock.style;

import static dev.storyblock.style.StyleFeatureAnalyzer.SENTENCE_PUNCTUATION;
import static dev.storyblock.style.StyleFeatureAnalyzer.CLAUSE_PUNCTUATION;

final class StyleFeatureAnalyzerShape {
    static String shape(String grapheme) {
        int codePoint = grapheme.codePointAt(0);
        if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
            return "HAN";
        }
        if (Character.isLetter(codePoint)) {
            return "LETTER";
        }
        if (Character.isDigit(codePoint)) {
            return "NUMBER";
        }
        if (SENTENCE_PUNCTUATION.contains(grapheme)) {
            return "SENTENCE_END";
        }
        if (CLAUSE_PUNCTUATION.contains(grapheme)) {
            return "CLAUSE_END";
        }
        return "OTHER";
    }
}
