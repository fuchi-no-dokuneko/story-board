package dev.storyblock.rewrite.policy;

import dev.storyblock.contracts.CanonicalJson;
import java.util.Map;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.FactKey;

final class RewriteProtectedFactExtractorAdd {
    static void add(
            Map<FactKey, Integer> facts,
            ProtectedFactKind kind,
            String value,
            int count
    ) {
        String hash = CanonicalJson.hash(Map.of(
                "kind", kind.canonicalName(), "value", value
        ));
        facts.merge(new FactKey(kind, hash), count, Integer::sum);
    }
}
