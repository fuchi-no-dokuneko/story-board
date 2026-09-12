package dev.storyblock.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

final class UnicodeTextGraphemesFactory {
    static List<String> graphemes(String text)  {
        Objects.requireNonNull(text, "text");
        Matcher matcher = UnicodeText.GRAPHEME.matcher(Normalizer.normalize(text, Normalizer.Form.NFC));
        List<String> graphemes = new ArrayList<>();
        while (matcher.find()) {
            String grapheme = matcher.group();
            if (!UnicodeTextIsLineBreak.isLineBreak(grapheme)) {
                graphemes.add(grapheme);
            }
        }
        return List.copyOf(graphemes);
    }
}
