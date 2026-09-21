package dev.storyblock.nlp.zh;

import java.util.*;

/** Internal Jieba SEARCH adaptation. No overlapping INDEX tokens or global mutations. */
public final class JiebaTokenizer implements ChineseTokenizer {
    private static final class Shared {
        static final DagSegmenter SEGMENTER = new DagSegmenter(DictionarySnapshot.load(), new HmmSegmenter());
    }
    @Override public List<ChineseToken> tokenize(String text, Set<String> properNames) {
        Objects.requireNonNull(text, "text");
        int[] cps = text.codePoints().toArray(), offsets = new int[cps.length + 1];
        for (int i = 0; i < cps.length; i++) offsets[i + 1] = offsets[i] + Character.charCount(cps[i]);
        int[] names = new NameOverrides(Set.copyOf(properNames)).matches(cps);
        var result = new ArrayList<ChineseToken>();
        int i = 0;
        while (i < cps.length) {
            int end = i + 1;
            if (names[i] > i) {
                end = names[i]; add(result, text, offsets, i, end, ChineseToken.Kind.WORD, true);
            } else if (han(cps[i])) {
                while (end < cps.length && han(cps[end]) && names[end] == 0) end++;
                Shared.SEGMENTER.cut(cps, i, end, (a,b) -> add(result,text,offsets,a,b,ChineseToken.Kind.WORD,false));
            } else {
                var kind = Character.isWhitespace(cps[i]) ? ChineseToken.Kind.SPACE
                        : Character.isLetterOrDigit(cps[i]) ? ChineseToken.Kind.WORD : ChineseToken.Kind.PUNCTUATION;
                if (kind != ChineseToken.Kind.PUNCTUATION)
                    while (end < cps.length && names[end] == 0 && !han(cps[end])
                            && (kind == ChineseToken.Kind.SPACE ? Character.isWhitespace(cps[end])
                            : Character.isLetterOrDigit(cps[end]) || Character.getType(cps[end]) == Character.NON_SPACING_MARK)) end++;
                add(result, text, offsets, i, end, kind, false);
            }
            i = end;
        }
        return List.copyOf(result);
    }
    private static boolean han(int cp) { return Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN; }
    private static void add(List<ChineseToken> output, String text, int[] offsets, int start, int end,
            ChineseToken.Kind kind, boolean name) {
        output.add(new ChineseToken(text.substring(offsets[start], offsets[end]), offsets[start], offsets[end], kind, name));
    }
}
