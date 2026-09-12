package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.DeterministicRenderer;
import dev.storyblock.validator.DeterministicValidator;
import dev.storyblock.validator.ValidationCode;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public final class PreviewService {
    static final Set<ValidationCode> NON_CANONICAL_TEXT_CODES = Set.of(
            ValidationCode.BLOCK_TOO_LONG,
            ValidationCode.INVALID_SENTENCE_COUNT
    );

    final NarrativeEditor editor;
    final DeterministicValidator validator;
    final DeterministicRenderer renderer;

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
        return PreviewServicePreviewAction.preview(this, base, requestedOperation, candidateRevisionId, candidateCreatedAt);
    }

}
