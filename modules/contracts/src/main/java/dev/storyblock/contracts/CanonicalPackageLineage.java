package dev.storyblock.contracts;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import static dev.storyblock.contracts.CanonicalNovelPackage.*;

final class CanonicalPackageLineage {
    record Result(List<RevisionManifest> manifests, Set<Ids.RevisionId> revisionIds) {}
    static Result validate(CanonicalNovelPackage document) {
        var revisions = document.revisions();
        var manifest = document.manifest();
        Set<Ids.RevisionId> revisionIds = new HashSet<>();
        List<RevisionManifest> manifests = new ArrayList<>(revisions.size());
        for (int index = 0; index < revisions.size(); index++) {
            RevisionEntry entry = revisions.get(index);
            if (entry.sequence() != index) {
                throw new CanonicalPackageException("Revision sequences must be contiguous");
            }
            RevisionManifest current = NarrativeCanonicalMapper.fromCanonical(entry.revision());
            manifests.add(current);
            if (!current.novel().id().equals(manifest.novelId())) {
                throw new CanonicalPackageException("Revision belongs to another novel");
            }
            if (!revisionIds.add(current.id())) {
                throw new CanonicalPackageException("Duplicate revision ID " + current.id().value());
            }
            Ids.RevisionId expectedParent = index == 0 ? null : manifests.get(index - 1).id();
            if (!Objects.equals(expectedParent, current.parentId())) {
                throw new CanonicalPackageException("Revision lineage is not a single ordered chain");
            }
        }

        return new Result(manifests, revisionIds);
    }

}
