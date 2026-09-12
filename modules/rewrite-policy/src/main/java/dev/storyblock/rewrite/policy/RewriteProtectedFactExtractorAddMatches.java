package dev.storyblock.rewrite.policy;

import java.util.Map;
import java.util.regex.Matcher;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.FactKey;

final class RewriteProtectedFactExtractorAddMatches {
    static void addMatches(
            Map<FactKey, Integer> facts,
            ProtectedFactKind kind,
            Matcher matcher
    ) {
        while (matcher.find()) {
            RewriteProtectedFactExtractorAdd.add(facts, kind, matcher.group(), 1);
        }
    }
}
