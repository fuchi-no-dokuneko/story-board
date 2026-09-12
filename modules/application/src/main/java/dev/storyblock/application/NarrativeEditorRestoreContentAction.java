package dev.storyblock.application;

import dev.storyblock.contracts.NarrativeCanonicalMapper;
import dev.storyblock.domain.EditInvariantException;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.NarrativeNovel;
import dev.storyblock.domain.RevisionManifest;

final class NarrativeEditorRestoreContentAction {
    static NarrativeNovel restoreContent(NarrativeEditor self, RevisionManifest base, EditOperation.RestoreRevisionContent operation)  {
        RevisionManifest target = self.revisionLookup.require(operation.restoreRevisionId());
        if (!base.novel().id().equals(target.novel().id())) {
            throw NarrativeEditorInvalid.invalid("Cannot restore content from another novel");
        }
        String targetHash = NarrativeCanonicalMapper.toCanonical(target).contentHash();
        if (!targetHash.equals(operation.expectedRestoreHash())) {
            throw new EditInvariantException(
                    EditInvariantException.Code.REVISION_CONFLICT,
                    "Restore revision hash does not match the requested target"
            );
        }
        return target.novel();
    }
}
