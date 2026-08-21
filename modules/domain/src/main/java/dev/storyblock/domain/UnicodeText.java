package dev.storyblock.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UnicodeText {
    public static final int MAX_BLOCK_GRAPHEMES = 100;
    public static final String PARSER_VERSION = "sentence-boundary-1.0.0";
    public static final String NORMALIZATION_VERSION = "nfc-view-1.0.0";

    private static final Pattern GRAPHEME = Pattern.compile("\\X");
    private static final String TERMINATOR =
            "(?:[。！？!?]+|(?<!\\d)\\.{1,3}(?!\\d)|…{1,2})";
    private static final String CLOSING_MARKS = "[」』”’\\\"'）)】》〉〕］}]*";
    private static final Pattern SENTENCE_END = Pattern.compile(TERMINATOR + CLOSING_MARKS);
    private static final Pattern COMPLETE_END = Pattern.compile(
            TERMINATOR + CLOSING_MARKS + "\\s*$"
    );

    private UnicodeText() {
    }

    public static TextAnalysis analyze(String text) {
        return analyze(text, List.of());
    }

    public static TextAnalysis analyze(String text, Collection<Integer> authorSplitAnchors) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(authorSplitAnchors, "authorSplitAnchors");
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        int graphemeCount = graphemeCount(normalized);
        Matcher sentenceMatcher = SENTENCE_END.matcher(normalized);
        List<Integer> boundaries = new ArrayList<>();
        while (sentenceMatcher.find()) {
            boundaries.add(graphemeCount(normalized.substring(0, sentenceMatcher.end())));
        }

        boolean complete = !normalized.isBlank() && COMPLETE_END.matcher(normalized).find();
        int sentenceCount = boundaries.size();
        TreeSet<Integer> safeAnchors = new TreeSet<>(boundaries);
        if (complete && !boundaries.isEmpty()) {
            safeAnchors.remove(boundaries.getLast());
        }
        for (Integer anchor : authorSplitAnchors) {
            if (anchor == null || anchor <= 0 || anchor >= graphemeCount) {
                throw new IllegalArgumentException(
                        "Author split anchors must be internal grapheme offsets"
                );
            }
            safeAnchors.add(anchor);
        }
        return new TextAnalysis(
                PARSER_VERSION,
                NORMALIZATION_VERSION,
                normalized,
                graphemeCount,
                sentenceCount,
                complete,
                List.copyOf(safeAnchors)
        );
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
            if (!isLineBreak(grapheme)) {
                count++;
            }
        }
        return count;
    }

    public static List<String> graphemes(String text) {
        Objects.requireNonNull(text, "text");
        Matcher matcher = GRAPHEME.matcher(Normalizer.normalize(text, Normalizer.Form.NFC));
        List<String> graphemes = new ArrayList<>();
        while (matcher.find()) {
            String grapheme = matcher.group();
            if (!isLineBreak(grapheme)) {
                graphemes.add(grapheme);
            }
        }
        return List.copyOf(graphemes);
    }

    private static boolean isLineBreak(String grapheme) {
        return grapheme.equals("\n") || grapheme.equals("\r") || grapheme.equals("\r\n");
    }
}
