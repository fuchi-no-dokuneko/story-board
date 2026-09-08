package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.DeterministicRenderer;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import dev.storyblock.renderer.RenderedBlock;
import dev.storyblock.renderer.ResolvedBlockMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AdjacentMetadataDetector {
    static final List<String> OBSERVATION_FIELDS = List.of(
            "location", "weather", "time", "pov"
    );

    private final DeterministicRenderer renderer;

    public AdjacentMetadataDetector() {
        this(new DeterministicRenderer());
    }

    public AdjacentMetadataDetector(DeterministicRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public DetectorRun detect(
            RevisionManifest revision,
            String revisionHash,
            RenderRange requestedRange
    ) {
        Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(requestedRange, "requestedRange");

        RenderPacket packet = renderer.render(revision, revisionHash, RenderRange.all());
        List<NarrativeScene> scenes = AdjacentMetadataDetectorScenes.scenes(revision);
        List<BlockContext> blocks = AdjacentMetadataDetectorBlockContexts.blockContexts(scenes, packet);
        Selection selection = AdjacentMetadataDetectorSelect.select(blocks, scenes, requestedRange);
        List<DetectorFinding> findings = new ArrayList<>();

        for (int index = selection.fromBlock(); index <= selection.toBlock(); index++) {
            AdjacentMetadataDetectorInspectBlock.inspectBlock(revision, packet, blocks, index, findings);
        }
        AdjacentMetadataDetectorInspectBoundaries.inspectBoundaries(revision, packet, scenes, selection, findings);

        return new DetectorRun(
                revision.id(),
                revisionHash,
                DetectorModule.VERSION,
                List.copyOf(findings)
        );
    }

    record Selection(
            int fromBlock,
            int toBlock,
            int firstScene,
            int lastScene,
            boolean all
    ) {
        boolean includesBoundary(int currentScene) {
            return all || (currentScene >= firstScene && currentScene <= lastScene);
        }
    }

    record BlockContext(
            NarrativeScene scene,
            int sceneIndex,
            boolean firstInScene,
            RenderedBlock rendered,
            ResolvedBlockMetadata resolved
    ) {
        Ids.BlockId blockId() {
            return rendered.blockId();
        }

        String text() {
            return rendered.text();
        }
    }
}
