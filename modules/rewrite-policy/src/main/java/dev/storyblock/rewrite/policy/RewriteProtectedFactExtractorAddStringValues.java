package dev.storyblock.rewrite.policy;

import java.util.Collection;
import java.util.Map;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.FactKey;

final class RewriteProtectedFactExtractorAddStringValues {
    static void addStringValues(
            Map<FactKey, Integer> facts,
            ProtectedFactKind kind,
            Object value
    ) {
        if (value instanceof String text && !text.isBlank()) {
            RewriteProtectedFactExtractorAdd.add(facts, kind, text, 1);
        } else if (value instanceof Collection<?> values) {
            values.forEach(entry -> addStringValues(facts, kind, entry));
        }
    }
}
