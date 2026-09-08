package dev.storyblock.application;

import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.DeterministicRenderer;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import dev.storyblock.validator.DeterministicValidator;
import dev.storyblock.validator.ValidationCode;
import dev.storyblock.validator.ValidationReport;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PreviewService {
    private static final Set<ValidationCode> NON_CANONICAL_TEXT_CODES = Set.of(
            ValidationCode.BLOCK_TOO_LONG,
            ValidationCode.INVALID_SENTENCE_COUNT
    );

    private final NarrativeEditor editor;
    private final DeterministicValidator validator;
    private final DeterministicRenderer renderer;

    public PreviewService(RevisionLookup revisionLookup) {
        this(
                new NarrativeEditor(revisionLookup),
                new DeterministicValidator(),
                new DeterministicRenderer()
        );
    }

    PreviewService(
            NarrativeEditor editor,
            DeterministicValidator validator,
            DeterministicRenderer renderer
    ) {
        this.editor = Objects.requireNonNull(editor, "editor");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public PreviewResponse preview(
            RevisionManifest base,
            EditOperation requestedOperation,
            Ids.RevisionId candidateRevisionId,
            Instant candidateCreatedAt
    ) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(requestedOperation, "requestedOperation");
        String baseHash = NarrativeCanonicalMapper.toCanonical(base).contentHash();
        ValidationReport operationReport = validator.validateOperation(
                base, baseHash, requestedOperation
        );
        EditOperation normalized = EditOperationNormalizer.normalize(requestedOperation);
        Map<String, Object> normalizedMap = EditOperationCanonicalMapper.toCanonical(normalized);

        if (!operationReport.committable()) {
            return PreviewServiceRejected.rejected(base, baseHash, normalizedMap, operationReport);
        }

        ValidationReport proposedBlocks = validator.validateOperationCandidates(
                base, baseHash, normalized
        );
        if (proposedBlocks.violations().stream()
                .anyMatch(issue -> NON_CANONICAL_TEXT_CODES.contains(issue.code()))) {
            return PreviewServiceRejected.rejected(base, baseHash, normalizedMap, proposedBlocks);
        }

        RevisionManifest candidate = editor.apply(
                base, normalized, candidateRevisionId, candidateCreatedAt
        );
        String candidateHash = NarrativeCanonicalMapper.toCanonical(candidate).contentHash();
        ValidationReport candidateReport = validator.validateRevision(candidate, base, candidateHash);
        ValidationReport combined = PreviewServiceDeduplicate.deduplicate(proposedBlocks.plus(candidateReport));
        RenderPacket packet = renderer.render(candidate, candidateHash, RenderRange.all());
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
