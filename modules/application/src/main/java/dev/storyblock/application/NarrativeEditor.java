package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.RevisionManifest;
import java.time.Instant;
import java.util.Objects;

public final class NarrativeEditor {
    final RevisionLookup revisionLookup;

    public NarrativeEditor(RevisionLookup revisionLookup) {
        this.revisionLookup = Objects.requireNonNull(revisionLookup, "revisionLookup");
    }

    public RevisionManifest apply(
            RevisionManifest base,
            EditOperation operation,
            Ids.RevisionId newRevisionId,
            Instant createdAt
    ) {
        return NarrativeEditorApplyAction.apply(this, base, operation, newRevisionId, createdAt);
    }

    NarrativeNovel restoreContent(
            RevisionManifest base,
            EditOperation.RestoreRevisionContent operation
    ) {
        return NarrativeEditorRestoreContentAction.restoreContent(this, base, operation);
    }

}
