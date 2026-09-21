package dev.storyblock.style.quality;

import dev.storyblock.nlp.zh.*;
import java.util.*;

/** One tokenization supplies every metric, window and supporting statistic. */
public final class QualityAnalysis {
    private QualityAnalysis() { }
    public static QualityWindow analyze(String text, QualityContract contract, ChineseTokenizer tokenizer) {
        var sentences = new ArrayList<QualitySentence>();
        var current = new ArrayList<QualityToken>();
        int words = 0;
        boolean boundary = false;
        for (var token : tokenizer.tokenize(text, contract.properNames())) {
            if (boundary && (token.kind() == ChineseToken.Kind.WORD || "「『“（(\"".contains(token.text()))) {
                append(sentences, current); current.clear(); boundary = false;
            }
            String normalized = token.text().toLowerCase(Locale.ROOT);
            boolean word = token.kind() == ChineseToken.Kind.WORD;
            current.add(new QualityToken(token, normalized, word && contract.functionWords().contains(normalized), word ? words++ : -1));
            if (token.text().codePoints().anyMatch(cp -> "。！？.!?\n\r".indexOf(cp) >= 0)) boundary = true;
        }
        append(sentences, current);
        return QualityWindow.of(text, sentences);
    }
    private static void append(List<QualitySentence> output, List<QualityToken> tokens) {
        var words = tokens.stream().filter(QualityToken::word).toList();
        if (!words.isEmpty()) output.add(new QualitySentence(output.size(), tokens.getFirst().source().start(),
                tokens.getLast().source().end(), tokens, words));
    }
    public static List<QualityWindow> windows(QualityWindow analysis, QualityContract contract) {
        var result = new ArrayList<QualityWindow>();
        var sentences = analysis.sentences();
        for (int start = 0; start < sentences.size();) {
            int end = start, count = 0;
            while (end < sentences.size() && count < contract.windowWords()) count += sentences.get(end++).words().size();
            result.add(QualityWindow.of(analysis.text(), sentences.subList(start, end)));
            if (end == sentences.size()) break;
            int advance = 0;
            do { advance += sentences.get(start++).words().size(); }
            while (start < end && advance < contract.strideWords());
        }
        return List.copyOf(result);
    }
}
