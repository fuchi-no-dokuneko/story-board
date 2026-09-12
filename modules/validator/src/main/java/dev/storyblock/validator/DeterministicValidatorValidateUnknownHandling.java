package dev.storyblock.validator;

import dev.storyblock.domain.BlockMetadata;
import dev.storyblock.domain.Ids;
import java.util.List;
import java.util.Map;
import static dev.storyblock.validator.ValidationVocabulary.OBSERVATION_FIELDS;

final class DeterministicValidatorValidateUnknownHandling {
    static void validateUnknownHandling(
            Ids.BlockId blockId,
            String text,
            BlockMetadata candidate,
            BlockMetadata baseline,
            List<ValidationIssue> issues
    ) {
        for (String field : OBSERVATION_FIELDS) {
            Object candidateValue = candidate.fields().get(field);
            if (!DeterministicValidatorIsMode.isMode(candidateValue, "explicit")) {
                continue;
            }
            boolean wasUnknown = baseline != null && DeterministicValidatorIsMode.isMode(baseline.fields().get(field), "unknown");
            boolean extractorAuthored = DeterministicValidatorIsExtractorAuthored.isExtractorAuthored(candidate.fields().get("provenance"));
            if ((wasUnknown || extractorAuthored) && !DeterministicValidatorHasEvidence.hasEvidence(candidateValue, text)
                    && !DeterministicValidatorHasProvenanceEvidence.hasProvenanceEvidence(candidate.fields().get("provenance"), text)) {
                issues.add(ValidationIssue.error(
                        ValidationCode.UNKNOWN_META_VALUE_INVENTED,
                        blockId,
                        "Unknown metadata cannot become explicit without evidence",
                        Map.of(
                                "field", field,
                                "previous_mode", wasUnknown ? "unknown" : "not_present",
                                "candidate_mode", "explicit"
                        )
                ));
            }
        }
    }
}
