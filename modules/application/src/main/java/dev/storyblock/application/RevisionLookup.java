package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;

@FunctionalInterface
public interface RevisionLookup {
    RevisionManifest require(Ids.RevisionId revisionId);
}
