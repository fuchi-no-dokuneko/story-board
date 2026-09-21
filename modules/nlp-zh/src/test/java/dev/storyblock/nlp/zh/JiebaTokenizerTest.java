package dev.storyblock.nlp.zh;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JiebaTokenizerTest {
    private final ChineseTokenizer tokenizer = new JiebaTokenizer();
    @Test void searchHasOneExclusivePathAndTraditionalDictionaryWords() {
        assertEquals(List.of("南京市","长江大桥"),tokenizer.tokenize("南京市长江大桥").stream().map(ChineseToken::text).toList());
        var traditional = tokenizer.tokenize("她把滑鼠放在電腦旁邊。");
        assertTrue(traditional.stream().anyMatch(t -> t.text().equals("滑鼠")));
        assertTrue(traditional.stream().anyMatch(t -> t.text().equals("電腦")));
        assertFalse(tokenizer.tokenize("南京市").stream().anyMatch(t -> t.text().equals("南京")));
        assertEquals(List.of("杭研","大厦"),tokenizer.tokenize("杭研大厦").stream().map(ChineseToken::text).toList());
    }
    @Test void supplementaryHanEmojiAndCombiningTextRetainOriginalOffsets() {
        String text = "𠮷野看見𠀀字，說：café🙂。\n臺灣";
        var tokens = tokenizer.tokenize(text,Set.of("𠮷野")); int offset = 0;
        for (var token : tokens) {
            assertEquals(offset,token.start()); assertEquals(text.substring(token.start(),token.end()),token.text());
            assertFalse(Character.isLowSurrogate(token.text().charAt(0))); offset = token.end();
        }
        assertEquals(text.length(),offset);
        assertTrue(tokens.stream().anyMatch(t -> t.text().equals("𠮷野") && t.properName()));
        assertTrue(tokens.stream().anyMatch(t -> t.text().equals("𠀀") && t.kind() == ChineseToken.Kind.WORD));
    }
    @Test void nameOverridesAreIsolatedAcrossConcurrentAnalyses() throws Exception {
        String text = "南京市长江大桥";
        var base = tokenizer.tokenize(text);
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<List<ChineseToken>>>();
            for (int i = 0; i < 32; i++) {
                boolean named = i % 2 == 0;
                tasks.add(() -> tokenizer.tokenize(text,named ? Set.of(text) : Set.of()));
            }
            var results = pool.invokeAll(tasks);
            for (int i = 0; i < results.size(); i++) {
                var tokens = results.get(i).get();
                if (i % 2 == 0) { assertEquals(1,tokens.size()); assertTrue(tokens.getFirst().properName()); }
                else assertEquals(base,tokens);
            }
        }
        assertEquals(base,tokenizer.tokenize(text));
    }
}
