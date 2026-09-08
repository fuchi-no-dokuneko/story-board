package dev.storyblock.validator;

import dev.storyblock.domain.Ids;
import java.util.List;
import java.util.Map;

final class DeterministicValidatorValidateEvidence {
    static void validateEvidence(
            Ids.BlockId blockId,
            String text,
            Map<String, Object> metadata,
            List<Map<String, Object>> events,
            List<ValidationIssue> issues
    ) {
        for (int index = 0; index < events.size(); index++) {
            Map<String, Object> event = events.get(index);
            Object evidence = event.get("evidence");
            if (!(evidence instanceof Map<?, ?> span)) {
                issues.add(ValidationIssue.error(
                        ValidationCode.META_EVIDENCE_REQUIRED,
                        blockId,
                        "Presence events require same-block evidence",
                        Map.of("event_index", index, "event_type", String.valueOf(event.get("type")))
                ));
            } else if (!EvidenceSpans.matches(text, span)) {
                issues.add(ValidationIssue.error(
                        ValidationCode.META_EVIDENCE_REQUIRED,
                        blockId,
                        "Presence event evidence is not valid for the current block",
                        Map.of("event_index", index, "reason", "stale_or_invalid_span")
                ));
                issues.add(DeterministicValidatorStaleEvidence.staleEvidence(blockId, "presence_events[" + index + "].evidence"));
            }
        }

        Object provenance = metadata.get("provenance");
        if (provenance instanceof Map<?, ?> provenanceMap) {
            Object evidence = provenanceMap.get("evidence");
            if (evidence instanceof List<?> spans) {
                for (int index = 0; index < spans.size(); index++) {
                    Object entry = spans.get(index);
                    if (!(entry instanceof Map<?, ?> span) || !EvidenceSpans.matches(text, span)) {
                        issues.add(DeterministicValidatorStaleEvidence.staleEvidence(blockId, "provenance.evidence[" + index + "]"));
                    }
                }
            }
        }
    }
}
