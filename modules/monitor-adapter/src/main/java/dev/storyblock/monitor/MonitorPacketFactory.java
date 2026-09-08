package dev.storyblock.monitor;

import dev.storyblock.detector.AdjacentMetadataDetector;
import dev.storyblock.detector.DetectorFinding;
import dev.storyblock.detector.DetectorRun;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.DeterministicRenderer;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MonitorPacketFactory {
    private final DeterministicRenderer renderer;
    private final AdjacentMetadataDetector detector;

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
        Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(targetBlockId, "targetBlockId");
        if (neighborCount < 1 || neighborCount > MonitorModule.MAX_NEIGHBORS) {
            throw new IllegalArgumentException("Monitor neighbor count must be 1 or 2");
        }

        List<BlockLocation> blocks = MonitorPacketFactoryFlatten.flatten(revision);
        int targetIndex = MonitorPacketFactoryIndexOf.indexOf(blocks, targetBlockId);
        int from = Math.max(0, targetIndex - neighborCount);
        int to = Math.min(blocks.size() - 1, targetIndex + neighborCount);
        RenderRange range = RenderRange.inclusive(
                blocks.get(from).block().id(), blocks.get(to).block().id()
        );
        RenderPacket render = MonitorPacketFactoryBoundedSceneState.boundedSceneState(
                renderer.render(revision, revisionHash, range), blocks, from, to
        );
        DetectorRun detectorRun = detector.detect(revision, revisionHash, range);
        Set<Ids.BlockId> windowIds = new HashSet<>();
        List<MonitorBlockFingerprint> fingerprints = new ArrayList<>();
        for (int index = from; index <= to; index++) {
            NarrativeBlock block = blocks.get(index).block();
            windowIds.add(block.id());
            fingerprints.add(MonitorBlockFingerprint.from(block));
        }
        List<DetectorFinding> localFindings = detectorRun.findings().stream()
                .filter(finding -> !finding.affectedBlockIds().isEmpty())
                .filter(finding -> windowIds.containsAll(finding.affectedBlockIds()))
                .filter(finding -> windowIds.containsAll(finding.contextBlockIds()))
                .toList();
        BlockLocation target = blocks.get(targetIndex);
        return new MonitorPacket(
                revision.novel().id(),
                revision.id(),
                revisionHash,
                MonitorModule.VERSION,
                MonitorModule.RULE_VERSION,
                detectorRun.ruleVersion(),
                targetBlockId,
                neighborCount,
                render,
                localFindings,
                new MonitorLocalInvariants(
                        target.sceneId(), target.block().versionId(), fingerprints
                ),
                List.of(
                        MonitorTool.SUBMIT_FINDING,
                        MonitorTool.SUBMIT_PROPOSED_OPERATION
                )
        );
    }

    record BlockLocation(Ids.SceneId sceneId, NarrativeBlock block) {
    }
}
