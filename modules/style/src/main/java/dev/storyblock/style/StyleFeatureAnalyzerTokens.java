package dev.storyblock.style;

import dev.storyblock.domain.UnicodeText;
import java.util.ArrayList;
import java.util.List;

final class StyleFeatureAnalyzerTokens {
    static List<String> tokens(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (String grapheme : UnicodeText.graphemes(text)) {
            int codePoint = grapheme.codePointAt(0);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                StyleFeatureAnalyzerFlush.flush(word, result);
                result.add(grapheme);
            } else if (Character.isLetterOrDigit(codePoint)) {
                word.append(grapheme.toLowerCase(java.util.Locale.ROOT));
            } else {
                StyleFeatureAnalyzerFlush.flush(word, result);
            }
        }
        StyleFeatureAnalyzerFlush.flush(word, result);
        return List.copyOf(result);
    }
}
