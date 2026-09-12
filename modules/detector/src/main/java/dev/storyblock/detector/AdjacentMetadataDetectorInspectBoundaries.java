package dev.storyblock.detector;

import dev.storyblock.domain.DerivedSceneBoundary;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.TransitionMode;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import static dev.storyblock.detector.AdjacentMetadataDetector.Selection;

final class AdjacentMetadataDetectorInspectBoundaries {
    static void inspectBoundaries(
            RevisionManifest revision,
            RenderPacket packet,
            List<NarrativeScene> scenes,
            Selection selection,
            List<DetectorFinding> findings
    ) {
        List<DerivedSceneBoundary> boundaries = packet.sceneBoundaries();
        if (boundaries.size() != scenes.size()) {
            throw new IllegalArgumentException("Render packet omitted a scene boundary");
        }
        for (int sceneIndex = 1; sceneIndex < scenes.size(); sceneIndex++) {
            if (!selection.includesBoundary(sceneIndex)) {
                continue;
            }
            NarrativeScene previousScene = scenes.get(sceneIndex - 1);
            NarrativeScene currentScene = scenes.get(sceneIndex);
            DerivedSceneBoundary previous = boundaries.get(sceneIndex - 1);
            DerivedSceneBoundary current = boundaries.get(sceneIndex);
            if (!previous.sceneId().equals(previousScene.id())
                    || !current.sceneId().equals(currentScene.id())) {
                throw new IllegalArgumentException("Render packet scene boundaries are out of order");
            }

            List<Ids.BlockId> boundaryBlocks = AdjacentMetadataDetectorBoundaryBlockIds.boundaryBlockIds(previousScene, currentScene);
            List<Ids.SceneId> boundaryScenes = List.of(previousScene.id(), currentScene.id());
            if (currentScene.transitionMode() == TransitionMode.CONTINUOUS) {
                AdjacentMetadataDetectorInspectContinuousBoundary.inspectContinuousBoundary(
                        revision,
                        packet,
                        previous.stateOut(),
                        current.stateIn(),
                        boundaryBlocks,
                        boundaryScenes,
                        findings
                );
            } else {
                AdjacentMetadataDetectorInspectResetBoundary.inspectResetBoundary(
                        revision,
                        packet,
                        currentScene.transitionMode(),
                        previous.stateOut(),
                        current.stateIn(),
                        boundaryBlocks,
                        boundaryScenes,
                        findings
                );
            }
        }
    }
}
