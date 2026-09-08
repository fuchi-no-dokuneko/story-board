package dev.storyblock.application;

import dev.storyblock.contracts.EditOperationCanonicalMapper;
import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.BlockDraft;
import dev.storyblock.domain.BlockImage;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.CommitRequest;
import dev.storyblock.storage.CommitResult;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StoredOperation;
import dev.storyblock.storage.StoredRevision;
import dev.storyblock.storage.StoredArtifact;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class CommitService {
    private final RevisionStore store;

    public CommitService(RevisionStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public CommitResult commit(
            EditOperation requestedOperation,
            Ids.RevisionId candidateRevisionId,
            Instant committedAt
    ) {
        Objects.requireNonNull(requestedOperation, "requestedOperation");
        return commit(
                requestedOperation,
                candidateRevisionId,
                committedAt,
                AuditContext.system(
                        "req_internal_" + requestedOperation.context().operationId().value(),
                        committedAt
                )
        );
    }

    public CommitResult commit(
            EditOperation requestedOperation,
            Ids.RevisionId candidateRevisionId,
            Instant committedAt,
            AuditContext auditContext
    ) {
        Objects.requireNonNull(requestedOperation, "requestedOperation");
        Objects.requireNonNull(candidateRevisionId, "candidateRevisionId");
        Objects.requireNonNull(committedAt, "committedAt");
        Objects.requireNonNull(auditContext, "auditContext");

        EditOperation operation = EditOperationNormalizer.normalize(requestedOperation);
        String operationHash = EditOperationCanonicalMapper.hash(operation);
        Optional<StoredOperation> prior = store.findByIdempotencyKey(
                operation.context().novelId(), operation.context().idempotencyKey()
        );
        if (prior.isPresent()) {
            CommitResult result = CommitServicePriorResult.priorResult(operation, operationHash, prior.get());
            store.recordCommitReplayAudit(prior.get(), auditContext);
            return result;
        }

        StoredRevision base = store.getRevision(
                operation.context().novelId(), operation.context().baseRevisionId()
        );
        RevisionRef expectedHead = new RevisionRef(
                base.manifest().id(), base.sequence(), operation.context().expectedHeadHash()
        );
        RevisionRef actualHead = store.getHead(operation.context().novelId());
        if (!expectedHead.equals(actualHead)) {
            throw new StaleHeadException(expectedHead, actualHead);
        }
        validateImageReferences(operation);

        RevisionLookup lookup = revisionId -> store.getRevision(
                operation.context().novelId(), revisionId
        ).manifest();
        PreviewResponse preview = new PreviewService(lookup).preview(
                base.manifest(), operation, candidateRevisionId, committedAt
        );
        if (!preview.committable()) {
            throw new CommitRejectedException(preview);
        }

        RevisionManifest candidate = new NarrativeEditor(lookup).apply(
                base.manifest(), operation, candidateRevisionId, committedAt
        );
        String candidateHash = NarrativeCanonicalMapper.toCanonical(candidate).contentHash();
        if (!candidateHash.equals(preview.candidateHash())) {
            throw new IllegalStateException("Preview and commit candidate hashes diverged");
        }

        return store.commitCas(
                new CommitRequest(
                        expectedHead, operation, operationHash, candidate, candidateHash
                ),
                auditContext
        );
    }

    private void validateImageReferences(EditOperation operation) {
        for (BlockDraft draft : CommitServiceCandidateDrafts.candidateDrafts(operation)) {
            draft.image().ifPresent(image -> validateImageReference(
                    operation.context().novelId(), image
            ));
        }
    }

    private void validateImageReference(Ids.NovelId novelId, BlockImage image) {
        StoredArtifact artifact = store.getArtifact(image.artifactId());
        ImageUploadService.ImageInfo decoded = ImageUploadService.inspect(
                artifact.content()
        );
        if (!artifact.novelId().equals(novelId)
                || !artifact.portable()
                || !"narrative-image".equals(artifact.kind())
                || !artifact.mediaType().equals(image.mediaType())
                || !artifact.contentHash().equals(image.contentHash())
                || !decoded.mediaType().equals(image.mediaType())
                || decoded.widthPixels() != image.widthPixels()
                || decoded.heightPixels() != image.heightPixels()) {
            throw new IllegalArgumentException(
                    "Image block must reference a matching portable image artifact in this novel"
            );
        }
    }

}
