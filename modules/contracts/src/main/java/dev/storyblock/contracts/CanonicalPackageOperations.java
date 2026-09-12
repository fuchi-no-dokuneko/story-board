package dev.storyblock.contracts;

import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static dev.storyblock.contracts.CanonicalNovelPackage.*;

final class CanonicalPackageOperations {
    static void validate(CanonicalNovelPackage document, List<RevisionManifest> manifests) {
        var revisions = document.revisions();
        var operations = document.operations();
        var manifest = document.manifest();
        Set<Ids.OperationId> operationIds = new HashSet<>();
        Set<String> idempotencyKeys = new HashSet<>();
        for (int index = 0; index < operations.size(); index++) {
            OperationEntry entry = operations.get(index);
            long expectedSequence = index + 1L;
            if (entry.sequence() != expectedSequence) {
                throw new CanonicalPackageException("Operation sequences must be contiguous");
            }
            EditOperation operation = entry.operation();
            RevisionEntry base = revisions.get(index);
            RevisionEntry result = revisions.get(index + 1);
            RevisionManifest resultManifest = manifests.get(index + 1);
            if (!operation.context().novelId().equals(manifest.novelId())
                    || !operation.context().baseRevisionId().equals(manifests.get(index).id())
                    || !operation.context().expectedHeadHash().equals(base.revision().contentHash())
                    || !entry.resultRevisionId().equals(resultManifest.id())
                    || !entry.resultHash().equals(result.revision().contentHash())
                    || !entry.committedAt().equals(resultManifest.createdAt())) {
                throw new CanonicalPackageException(
                        "Operation does not match its base and result revisions"
                );
            }
            if (!EditOperationCanonicalMapper.hash(operation).equals(entry.operationHash())) {
                throw new CanonicalPackageException("Operation hash does not match its payload");
            }
            if (!operationIds.add(operation.context().operationId())) {
                throw new CanonicalPackageException("Duplicate operation ID");
            }
            if (!idempotencyKeys.add(operation.context().idempotencyKey())) {
                throw new CanonicalPackageException("Duplicate operation idempotency key");
            }
        }

    }

}
