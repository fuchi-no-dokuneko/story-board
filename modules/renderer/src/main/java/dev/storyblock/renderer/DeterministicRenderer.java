package dev.storyblock.renderer;

import dev.storyblock.domain.DerivedSceneBoundary;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.RevisionManifest;
import java.util.List;
import java.util.regex.Pattern;

public final class DeterministicRenderer {
    static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public RenderPacket render(
            RevisionManifest revision,
            String revisionHash,
            RenderRange requestedRange
    ) {
        return DeterministicRendererRenderAction.render(this, revision, revisionHash, requestedRange);
    }

    record Resolution(
            List<ResolvedEntry> entries,
            List<DerivedSceneBoundary> sceneBoundaries
    ) {
    }

    record ResolvedEntry(
            int sceneIndex,
            NarrativeBlock block,
            ResolvedBlockMetadata resolved
    ) {
    }
}
