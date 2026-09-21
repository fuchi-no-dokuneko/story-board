package dev.storyblock.application;

import dev.storyblock.domain.*;
import java.util.HashMap;

/** Replay uses recorded identities; collision allocation is only for new edits. */
final class ReplayApplication {
    static RevisionManifest apply(NarrativeEditor editor, RevisionManifest base,
            EditOperation operation, RevisionManifest expected) {
        var versions = new HashMap<String, String>();
        for (var block : expected.liveBlocks()) {
            String key = "blv\0" + operation.context().operationId().value()
                    + "\0block-version:" + block.id().value();
            versions.put(key, block.versionId().value());
        }
        try (var scope = ShortIdScope.open((prefix, key) -> {
            String version = versions.get(key);
            if (!"blv".equals(prefix) || version == null)
                throw new IllegalArgumentException("Replay has no recorded block version");
            return version;
        })) {
            return editor.apply(base, operation, expected.id(), expected.createdAt());
        }
    }
}
