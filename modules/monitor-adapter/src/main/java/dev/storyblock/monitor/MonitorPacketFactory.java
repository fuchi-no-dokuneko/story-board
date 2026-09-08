package dev.storyblock.monitor;

import dev.storyblock.detector.AdjacentMetadataDetector;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.DeterministicRenderer;
import java.util.Objects;

public final class MonitorPacketFactory {
    final DeterministicRenderer renderer;
    final AdjacentMetadataDetector detector;

    public MonitorPacketFactory() {
        this(new DeterministicRenderer(), new AdjacentMetadataDetector());
    }

    public MonitorPacketFactory(
            DeterministicRenderer renderer,
            AdjacentMetadataDetector detector
    ) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.detector = Objects.requireNonNull(detector, "detector");
    }

    public MonitorPacket create(
            RevisionManifest revision,
            String revisionHash,
            Ids.BlockId targetBlockId,
            int neighborCount
    ) {
        return MonitorPacketFactoryCreateAction.create(this, revision, revisionHash, targetBlockId, neighborCount);
    }

    record BlockLocation(Ids.SceneId sceneId, NarrativeBlock block) {
    }
}
