package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.TransitionMode;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;

final class AdjacentMetadataDetectorAddBoundaryCharacterFinding {
    static void addBoundaryCharacterFinding(
            RevisionManifest revision,
            RenderPacket packet,
            FindingCode code,
            String characterId,
            List<Ids.BlockId> blockIds,
            List<Ids.SceneId> sceneIds,
            List<DetectorFinding> findings
    ) {
        AdjacentMetadataDetectorAddFinding.addFinding(
                revision,
                packet,
                code,
                blockIds,
                sceneIds,
                blockIds,
                Map.of(
                        "character_id", characterId,
                        "kind", "scene_boundary",
                        "transition_mode", TransitionMode.CONTINUOUS.canonicalName()
                ),
                findings
        );
    }
}
