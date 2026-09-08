package dev.storyblock.rewrite.policy;

import dev.storyblock.validator.EvidenceSpans;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.EVIDENCE_FIELDS;
import static dev.storyblock.rewrite.policy.RewriteProtectedFactExtractor.FactKey;

final class RewriteProtectedFactExtractorInspectEvidence {
    static void inspectEvidence(
            Object value,
            String text,
            Map<FactKey, Integer> facts,
            Set<String> manual
    ) {
        if (value instanceof Map<?, ?> map) {
            if (map.keySet().containsAll(EVIDENCE_FIELDS)) {
                Object quoteHash = map.get("quote_hash");
                if (EvidenceSpans.matches(text, map) && quoteHash instanceof String hash) {
                    RewriteProtectedFactExtractorAdd.add(facts, ProtectedFactKind.EVIDENCE, hash, 1);
                } else {
                    manual.add("stale_evidence");
                }
                return;
            }
            map.values().forEach(entry -> inspectEvidence(entry, text, facts, manual));
        } else if (value instanceof List<?> list) {
            list.forEach(entry -> inspectEvidence(entry, text, facts, manual));
        }
    }
}
