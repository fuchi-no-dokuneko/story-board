package dev.storyblock.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;

final class UnicodeTextAnalyzeFactory {
    static TextAnalysis analyze(String text, Collection<Integer> authorSplitAnchors)  {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(authorSplitAnchors, "authorSplitAnchors");
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        int graphemeCount = UnicodeText.graphemeCount(normalized);
        Matcher sentenceMatcher = UnicodeText.SENTENCE_END.matcher(normalized);
        List<Integer> boundaries = new ArrayList<>();
        while (sentenceMatcher.find()) {
            boundaries.add(UnicodeText.graphemeCount(normalized.substring(0, sentenceMatcher.end())));
        }

        boolean complete = !normalized.isBlank() && UnicodeText.COMPLETE_END.matcher(normalized).find();
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
                UnicodeText.PARSER_VERSION,
                UnicodeText.NORMALIZATION_VERSION,
                normalized,
                graphemeCount,
                sentenceCount,
                complete,
                List.copyOf(safeAnchors)
        );
    }
}
