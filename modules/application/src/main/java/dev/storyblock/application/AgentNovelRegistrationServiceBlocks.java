package dev.storyblock.application;

import dev.storyblock.domain.UnicodeText;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import static dev.storyblock.application.AgentNovelRegistrationService.SENTENCE;

final class AgentNovelRegistrationServiceBlocks {
    static List<String> blocks(String text, int chapterIndex) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC).strip();
        Matcher matcher = SENTENCE.matcher(normalized);
        List<String> sentences = new ArrayList<>();
        int consumed = 0;
        while (matcher.find()) {
            String sentence = matcher.group(1).stripTrailing();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            consumed = matcher.end();
        }
        String trailing = normalized.substring(consumed).strip();
        if (!trailing.isEmpty()) {
            sentences.add(trailing + "。");
        }
        if (sentences.isEmpty()) {
            throw new IllegalArgumentException("chapter " + chapterIndex + " has no sentences");
        }
        for (String sentence : sentences) {
            if (UnicodeText.graphemeCount(sentence) > UnicodeText.MAX_BLOCK_GRAPHEMES) {
                throw new IllegalArgumentException(
                        "chapter " + chapterIndex
                                + " contains a sentence longer than 100 graphemes"
                );
            }
        }
        List<String> result = new ArrayList<>();
        for (int index = 0; index < sentences.size();) {
            String block = sentences.get(index++).strip();
            if (index < sentences.size()) {
                String candidate = block + sentences.get(index);
                if (UnicodeText.graphemeCount(candidate) <= UnicodeText.MAX_BLOCK_GRAPHEMES) {
                    block = candidate;
                    index++;
                }
            }
            UnicodeText.validateBlock(block);
            result.add(block);
        }
        return List.copyOf(result);
    }
}
