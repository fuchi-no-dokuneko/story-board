package dev.storyblock.nlp.zh;

import java.util.List;
import java.util.Set;

/** Exclusive SEARCH segmentation; offsets are UTF-16 indices into the original text. */
public interface ChineseTokenizer {
    List<ChineseToken> tokenize(String text, Set<String> properNames);
    default List<ChineseToken> tokenize(String text) { return tokenize(text, Set.of()); }
}
