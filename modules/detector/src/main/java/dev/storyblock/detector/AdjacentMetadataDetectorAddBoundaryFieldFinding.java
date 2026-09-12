package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.TransitionMode;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;

final class AdjacentMetadataDetectorAddBoundaryFieldFinding {
    static void addBoundaryFieldFinding(
            RevisionManifest revision,
            RenderPacket packet,
            String field,
            FindingCode code,
            Map<String, Object> stateOut,
            Map<String, Object> stateIn,
            List<Ids.BlockId> blockIds,
            List<Ids.SceneId> sceneIds,
            List<DetectorFinding> findings
    ) {
        Object before = stateOut.get(field);
        Object after = stateIn.get(field);
        if (!AdjacentMetadataDetectorComparableChange.comparableChange(before, after)) {
            return;
        }
        AdjacentMetadataDetectorAddFinding.addFinding(
                revision,
                packet,
                code,
                blockIds,
                sceneIds,
                blockIds,
                Map.of(
                        "after", after,
                        "before", before,
                        "field", field,
                        "kind", "scene_boundary",
                        "transition_mode", TransitionMode.CONTINUOUS.canonicalName()
                ),
                findings
        );
    }
}
