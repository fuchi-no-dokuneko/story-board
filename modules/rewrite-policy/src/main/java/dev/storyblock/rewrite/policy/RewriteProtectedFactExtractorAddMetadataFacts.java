package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import java.util.Collection;
import java.util.Map;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.FactKey;

final class RewriteProtectedFactExtractorAddMetadataFacts {
    static void addMetadataFacts(
            Map<FactKey, Integer> facts,
            Object value,
            String field
    ) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object entry : collection) {
                RewriteProtectedFactExtractorAdd.add(facts, ProtectedFactKind.HIGH_RISK_METADATA,
                        field + ":" + CanonicalJson.string(entry), 1);
            }
        } else {
            RewriteProtectedFactExtractorAdd.add(facts, ProtectedFactKind.HIGH_RISK_METADATA,
                    field + ":" + CanonicalJson.string(value), 1);
        }
    }
}
