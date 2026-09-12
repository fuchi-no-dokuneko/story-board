package dev.storyblock.application;

import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import dev.storyblock.validator.ValidationReport;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

final class PreviewServicePreviewAction {
    static PreviewResponse preview(PreviewService self, RevisionManifest base, EditOperation requestedOperation, Ids.RevisionId candidateRevisionId, Instant candidateCreatedAt)  {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(requestedOperation, "requestedOperation");
        String baseHash = NarrativeCanonicalMapper.toCanonical(base).contentHash();
        ValidationReport operationReport = self.validator.validateOperation(
                base, baseHash, requestedOperation
        );
        EditOperation normalized = EditOperationNormalizer.normalize(requestedOperation);
        Map<String, Object> normalizedMap = EditOperationCanonicalMapper.toCanonical(normalized);

        if (!operationReport.committable()) {
            return PreviewServiceRejected.rejected(base, baseHash, normalizedMap, operationReport);
        }

        ValidationReport proposedBlocks = self.validator.validateOperationCandidates(
                base, baseHash, normalized
        );
        if (proposedBlocks.violations().stream()
                .anyMatch(issue -> PreviewService.NON_CANONICAL_TEXT_CODES.contains(issue.code()))) {
            return PreviewServiceRejected.rejected(base, baseHash, normalizedMap, proposedBlocks);
        }

        RevisionManifest candidate = self.editor.apply(
                base, normalized, candidateRevisionId, candidateCreatedAt
        );
        String candidateHash = NarrativeCanonicalMapper.toCanonical(candidate).contentHash();
        ValidationReport candidateReport = self.validator.validateRevision(candidate, base, candidateHash);
        ValidationReport combined = PreviewServiceDeduplicate.deduplicate(proposedBlocks.plus(candidateReport));
        RenderPacket packet = self.renderer.render(candidate, candidateHash, RenderRange.all());
        RevisionDiff diff = RevisionDiff.between(base, candidate);

        return new PreviewResponse(
                base.id(),
                baseHash,
                normalizedMap,
                candidateHash,
                diff,
                packet,
                combined.violations(),
                combined.warnings(),
                combined.committable()
        );
    }
}
