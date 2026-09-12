package dev.storyblock.style;

import dev.storyblock.domain.UnicodeText;
import java.text.Normalizer;
import java.util.*;

public final class ReferenceSegments {
    private ReferenceSegments() {}

    public static List<ReferenceText> split(String raw) {
        String text = Normalizer.normalize(raw, Normalizer.Form.NFC).strip();
        var result = new ArrayList<ReferenceText>();
        for (String paragraph : text.split("\\R+")) {
            if (paragraph.isBlank()) continue;
            var units = UnicodeText.graphemes(paragraph);
            var ends = new ArrayList<>(UnicodeText.analyze(paragraph).safeSplitAnchors());
            ends.add(units.size());
            int start = 0;
            String pending = null;
            for (int end : ends) {
                String sentence = String.join("", units.subList(start, end)).strip();
                start = end;
                if (sentence.isEmpty()) continue;
                if (pending == null) pending = sentence;
                else if (UnicodeText.graphemeCount(pending + sentence) <= UnicodeText.MAX_BLOCK_GRAPHEMES) {
                    result.add(new ReferenceText(pending + sentence)); pending = null;
                } else {
                    result.add(new ReferenceText(pending)); pending = sentence;
                }
            }
            if (pending != null) result.add(new ReferenceText(pending));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Reference files contain no text");
        return List.copyOf(result);
    }
}
