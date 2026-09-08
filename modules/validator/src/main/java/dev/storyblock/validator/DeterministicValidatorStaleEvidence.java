package dev.storyblock.validator;

import dev.storyblock.domain.Ids;
import java.util.Map;

final class DeterministicValidatorStaleEvidence {
    static ValidationIssue staleEvidence(Ids.BlockId blockId, String path) {
        return ValidationIssue.error(
                ValidationCode.EVIDENCE_SPAN_STALE,
                blockId,
                "Evidence span or quote hash does not match the current block text",
                Map.of("evidence_path", path)
        );
    }
}
