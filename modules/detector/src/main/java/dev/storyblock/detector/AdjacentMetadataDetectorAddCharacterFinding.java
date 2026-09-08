package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;
import static dev.storyblock.detector.AdjacentMetadataDetector.BlockContext;

final class AdjacentMetadataDetectorAddCharacterFinding {
    static void addCharacterFinding(
            RevisionManifest revision,
            RenderPacket packet,
            BlockContext current,
            List<Ids.BlockId> contextIds,
            FindingCode code,
            String characterId,
            String kind,
            List<DetectorFinding> findings
    ) {
        AdjacentMetadataDetectorAddFinding.addFinding(
                revision,
                packet,
                code,
                List.of(current.blockId()),
                List.of(current.scene().id()),
                contextIds,
                Map.of(
                        "character_id", characterId,
                        "kind", kind,
                        "transition_mode", current.scene().transitionMode().canonicalName()
                ),
                findings
        );
    }
}
