package dev.storyblock.rewrite.policy;

import java.util.ArrayList;
import java.util.List;
import static dev.storyblock.rewrite.policy.RewriteNearCopyChecker.NGRAM_GRAPHEMES;

final class RewriteNearCopyCheckerNgrams {
    static List<String> ngrams(List<String> units) {
        if (units.size() < NGRAM_GRAPHEMES) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (int index = 0; index + NGRAM_GRAPHEMES <= units.size(); index++) {
            result.add(String.join(
                    "", units.subList(index, index + NGRAM_GRAPHEMES)
            ));
        }
        return List.copyOf(result);
    }
}
