package dev.storyblock.application;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.Ids;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.CommitResult;
import dev.storyblock.storage.RevisionStore;
import java.time.Instant;
import java.util.Objects;

public final class CommitService {
    final RevisionStore store;

    public CommitService(RevisionStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public CommitResult commit(
            EditOperation requestedOperation,
            Ids.RevisionId candidateRevisionId,
            Instant committedAt
    ) {
        return CommitServiceCommitAction.commit(this, requestedOperation, candidateRevisionId, committedAt);
    }

    public CommitResult commit(
            EditOperation requestedOperation,
            Ids.RevisionId candidateRevisionId,
            Instant committedAt,
            AuditContext auditContext
    ) {
        return CommitServiceCommitAction.commit(this, requestedOperation, candidateRevisionId, committedAt, auditContext);
    }

    void validateImageReferences(EditOperation operation) {
        for (BlockDraft draft : CommitServiceCandidateDrafts.candidateDrafts(operation)) {
            draft.image().ifPresent(image -> validateImageReference(
                    operation.context().novelId(), image
            ));
        }
    }

    void validateImageReference(Ids.NovelId novelId, BlockImage image) {
        CommitServiceValidateImageReferenceAction.validateImageReference(this, novelId, image);
    }

}
