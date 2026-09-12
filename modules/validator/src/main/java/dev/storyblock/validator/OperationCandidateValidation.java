package dev.storyblock.validator;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import static dev.storyblock.validator.DeterministicValidator.Candidate;
import static dev.storyblock.validator.DeterministicValidator.BlockValidation;

final class OperationCandidateValidation {
    static ValidationReport validateOperationCandidates(DeterministicValidator self, RevisionManifest base, String baseHash, EditOperation operation)  {
        List<Candidate> candidates = self.candidates(base, baseHash, operation);
        List<ValidationIssue> issues = new ArrayList<>();
        Set<String> present = candidates.isEmpty()
                ? Set.of()
                : new TreeSet<>(candidates.getFirst().presentBefore());
        for (Candidate candidate : candidates) {
            BlockValidation validation = self.validateBlock(
                    candidate.blockId(),
                    candidate.text(),
                    candidate.metadata(),
                    present,
                    candidate.baselineMetadata()
            );
            issues.addAll(validation.issues());
            present = validation.presentAfter();
        }
        return DeterministicValidatorErrors.errors(issues);
    }
}
