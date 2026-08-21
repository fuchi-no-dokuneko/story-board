package dev.storyblock.monitor;

import dev.storyblock.detector.AdjacentMetadataDetector;
import dev.storyblock.detector.DetectorFinding;
import dev.storyblock.detector.DetectorRun;
import dev.storyblock.domain.DerivedSceneBoundary;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.DeterministicRenderer;
import dev.storyblock.renderer.RenderPacket;
import dev.storyblock.renderer.RenderRange;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

        List<BlockLocation> blocks = flatten(revision);
        int targetIndex = indexOf(blocks, targetBlockId);
        int from = Math.max(0, targetIndex - neighborCount);
        int to = Math.min(blocks.size() - 1, targetIndex + neighborCount);
        RenderRange range = RenderRange.inclusive(
                blocks.get(from).block().id(), blocks.get(to).block().id()
        );
        RenderPacket render = boundedSceneState(
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

    private static List<BlockLocation> flatten(RevisionManifest revision) {
        List<BlockLocation> result = new ArrayList<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                for (NarrativeBlock block : scene.blocks()) {
                    result.add(new BlockLocation(scene.id(), block));
                }
            }
        }
        return List.copyOf(result);
    }

    private static int indexOf(List<BlockLocation> blocks, Ids.BlockId targetBlockId) {
        for (int index = 0; index < blocks.size(); index++) {
            if (blocks.get(index).block().id().equals(targetBlockId)) {
                return index;
            }
        }
        throw new IllegalArgumentException(
                "Revision does not contain monitor target " + targetBlockId.value()
        );
    }

    private static RenderPacket boundedSceneState(
            RenderPacket render,
            List<BlockLocation> blocks,
            int from,
            int to
    ) {
        List<DerivedSceneBoundary> boundaries = new ArrayList<>();
        int globalIndex = from;
        int localIndex = 0;
        while (globalIndex <= to) {
            Ids.SceneId sceneId = blocks.get(globalIndex).sceneId();
            Map<String, Object> stateIn = render.resolvedMetadata().get(localIndex).before();
            Map<String, Object> stateOut = render.resolvedMetadata().get(localIndex).after();
            while (globalIndex + 1 <= to
                    && blocks.get(globalIndex + 1).sceneId().equals(sceneId)) {
                globalIndex++;
                localIndex++;
                stateOut = render.resolvedMetadata().get(localIndex).after();
            }
            boundaries.add(new DerivedSceneBoundary(sceneId, stateIn, stateOut));
            globalIndex++;
            localIndex++;
        }
        return new RenderPacket(
                render.novelId(),
                render.revisionId(),
                render.revisionHash(),
                render.rendererVersion(),
                render.range(),
                render.renderedText(),
                render.blocks(),
                render.resolvedMetadata(),
                render.offsetMap(),
                boundaries
        );
    }

    private record BlockLocation(Ids.SceneId sceneId, NarrativeBlock block) {
    }
}
