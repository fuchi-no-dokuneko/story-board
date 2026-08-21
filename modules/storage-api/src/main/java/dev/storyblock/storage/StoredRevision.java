package dev.storyblock.storage;

import dev.storyblock.domain.RevisionManifest;
import java.util.Objects;

public record StoredRevision(
        RevisionManifest manifest,
        long sequence,
        String contentHash
) {
    public StoredRevision {
        Objects.requireNonNull(manifest, "manifest");
        new RevisionRef(manifest.id(), sequence, contentHash);
    }

    public RevisionRef reference() {
        return new RevisionRef(manifest.id(), sequence, contentHash);
    }
}
