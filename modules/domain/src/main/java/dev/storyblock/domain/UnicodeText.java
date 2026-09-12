package dev.storyblock.domain;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UnicodeText {
    public static final int MAX_BLOCK_GRAPHEMES = 100;
    public static final String PARSER_VERSION = "sentence-boundary-1.0.0";
    public static final String NORMALIZATION_VERSION = "nfc-view-1.0.0";

    static final Pattern GRAPHEME = Pattern.compile("\\X");
    static final String TERMINATOR =
            "(?:[。！？!?]+|(?<!\\d)\\.{1,3}(?!\\d)|…{1,2})";
    static final String CLOSING_MARKS = "[」』”’\\\"'）)】》〉〕］}]*";
    static final Pattern SENTENCE_END = Pattern.compile(TERMINATOR + CLOSING_MARKS);
    static final Pattern COMPLETE_END = Pattern.compile(
            TERMINATOR + CLOSING_MARKS + "\\s*$"
    );

    UnicodeText() {
    }

    public static TextAnalysis analyze(String text) {
        return analyze(text, List.of());
    }

    public static TextAnalysis analyze(String text, Collection<Integer> authorSplitAnchors) {
        return UnicodeTextAnalyzeFactory.analyze(text, authorSplitAnchors);
    }

    public static TextAnalysis validateBlock(String text) {
        TextAnalysis analysis = analyze(text);
        if (!analysis.validBlockShape()) {
            throw new InvalidBlockTextException(analysis);
        }
        return analysis;
    }

    public static int graphemeCount(String text) {
        Objects.requireNonNull(text, "text");
        Matcher matcher = GRAPHEME.matcher(Normalizer.normalize(text, Normalizer.Form.NFC));
        int count = 0;
        while (matcher.find()) {
            String grapheme = matcher.group();
            if (!UnicodeTextIsLineBreak.isLineBreak(grapheme)) {
                count++;
            }
        }
        return count;
    }

    public static List<String> graphemes(String text) {
        return UnicodeTextGraphemesFactory.graphemes(text);
    }

}
