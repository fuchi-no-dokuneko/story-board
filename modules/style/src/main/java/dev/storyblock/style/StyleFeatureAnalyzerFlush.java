package dev.storyblock.style;

import java.util.List;

final class StyleFeatureAnalyzerFlush {
    static void flush(StringBuilder word, List<String> result) {
        if (!word.isEmpty()) {
            result.add(word.toString());
            word.setLength(0);
        }
    }
}
