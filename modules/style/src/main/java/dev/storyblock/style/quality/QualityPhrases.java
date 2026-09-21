package dev.storyblock.style.quality;

import java.util.*;
import java.util.function.Consumer;

final class QualityPhrases {
    record Phrase(List<String> key, List<QualityToken> words, int sentence) { }
    static void each(QualityWindow window, QualityContract contract, Consumer<Phrase> consume) {
        for (var sentence : window.sentences()) {
            var words = sentence.words();
            for (int start = 0; start + contract.minPhraseWords() <= words.size(); start++) {
                for (int size = contract.minPhraseWords(); size <= contract.maxPhraseWords() && start + size <= words.size(); size++) {
                    var span = words.subList(start, start + size);
                    consume.accept(new Phrase(span.stream().map(QualityToken::normalized).toList(), span, sentence.index()));
                }
            }
        }
    }
    static Set<List<String>> distinct(QualityWindow window, QualityContract contract) {
        var result = new HashSet<List<String>>(); each(window, contract, p -> result.add(p.key())); return result;
    }
}
