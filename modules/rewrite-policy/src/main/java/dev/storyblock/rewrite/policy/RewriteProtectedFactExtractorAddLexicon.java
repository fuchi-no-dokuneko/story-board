package dev.storyblock.rewrite.policy;

import java.util.List;
import java.util.Map;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.FactKey;

final class RewriteProtectedFactExtractorAddLexicon {
    static void addLexicon(
            Map<FactKey, Integer> facts,
            ProtectedFactKind kind,
            String text,
            List<String> values
    ) {
        for (String value : values) {
            int count = RewriteProtectedFactExtractorOccurrences.occurrences(text, value, false);
            if (count > 0) {
                RewriteProtectedFactExtractorAdd.add(facts, kind, value, count);
            }
        }
    }
}
