package dev.storyblock.application;

import dev.storyblock.contracts.*;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class CanonicalTransferServiceVerifyReplayFactory {
  static void verifyReplay(CanonicalNovelPackage document)  {
    Objects.requireNonNull(document, "document");
    Map<Ids.RevisionId, RevisionManifest> materialized = new LinkedHashMap<>();
    List<RevisionManifest> manifests = new ArrayList<>();
    RevisionManifest current = NarrativeCanonicalMapper.fromCanonical(
        document.revisions().getFirst().revision()
    );
    materialized.put(current.id(), current);
    manifests.add(current);

    NarrativeEditor editor = new NarrativeEditor(revisionId -> {
      RevisionManifest revision = materialized.get(revisionId);
      if (revision == null) {
        throw new CanonicalPackageException(
            "Operation references an unavailable historical revision "
                + revisionId.value()
        );
      }
      return revision;
    });
    for (int index = 0; index < document.operations().size(); index++) {
      CanonicalNovelPackage.OperationEntry operation = document.operations().get(index);
      CanonicalNovelPackage.RevisionEntry expected = document.revisions().get(index + 1);
      final RevisionManifest candidate;
      try {
        candidate = editor.apply(
            current,
            operation.operation(),
            operation.resultRevisionId(),
            operation.committedAt()
        );
      } catch (RuntimeException failure) {
        throw new CanonicalPackageException(
            "Canonical operation replay failed at sequence " + operation.sequence(),
            failure
        );
      }
      CanonicalRevision replayed = NarrativeCanonicalMapper.toCanonical(candidate);
      if (!MessageDigest.isEqual(replayed.envelopeBytes(), expected.revision().envelopeBytes())) {
        throw new CanonicalPackageException(
            "Canonical operation replay drift at sequence " + operation.sequence()
        );
      }
      materialized.put(candidate.id(), candidate);
      manifests.add(candidate);
      current = candidate;
    }
    if (!NarrativeCanonicalMapper.toCanonical(current).contentHash().equals(
        document.manifest().headHash()
    )) {
      throw new CanonicalPackageException("Full package replay does not match the head hash");
    }
    CanonicalTransferServiceValidateImageArtifacts.validateImageArtifacts(document, manifests);
  }
}
