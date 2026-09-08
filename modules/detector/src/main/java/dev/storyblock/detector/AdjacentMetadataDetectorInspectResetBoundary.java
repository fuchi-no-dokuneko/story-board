package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.TransitionMode;
import dev.storyblock.renderer.RenderPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static dev.storyblock.detector.AdjacentMetadataDetector.OBSERVATION_FIELDS;

final class AdjacentMetadataDetectorInspectResetBoundary {
    static void inspectResetBoundary(
            RevisionManifest revision,
            RenderPacket packet,
            TransitionMode mode,
            Map<String, Object> stateOut,
            Map<String, Object> stateIn,
            List<Ids.BlockId> blockIds,
            List<Ids.SceneId> sceneIds,
            List<DetectorFinding> findings
    ) {
        List<String> changedFields = new ArrayList<>();
        for (String field : OBSERVATION_FIELDS) {
            if (AdjacentMetadataDetectorComparableChange.comparableChange(stateOut.get(field), stateIn.get(field))) {
                changedFields.add(field);
            }
        }
        Set<String> before = AdjacentMetadataDetectorStrings.strings(stateOut.get("present_character_ids"));
        Set<String> after = AdjacentMetadataDetectorStrings.strings(stateIn.get("present_character_ids"));
        List<String> appeared = AdjacentMetadataDetectorDifference.difference(after, before);
        List<String> disappeared = AdjacentMetadataDetectorDifference.difference(before, after);
        if (changedFields.isEmpty() && appeared.isEmpty() && disappeared.isEmpty()) {
            return;
        }

        AdjacentMetadataDetectorAddFinding.addFinding(
                revision,
                packet,
                FindingCode.INTENTIONAL_SCENE_RESET,
                blockIds,
                sceneIds,
                blockIds,
                Map.of(
                        "appeared_character_ids", appeared,
                        "changed_fields", List.copyOf(changedFields),
                        "disappeared_character_ids", disappeared,
                        "kind", "scene_boundary",
                        "transition_mode", mode.canonicalName()
                ),
                findings
        );
    }
}
