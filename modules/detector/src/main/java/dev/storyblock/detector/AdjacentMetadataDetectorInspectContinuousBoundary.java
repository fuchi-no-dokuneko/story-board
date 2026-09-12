package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AdjacentMetadataDetectorInspectContinuousBoundary {
    static void inspectContinuousBoundary(
            RevisionManifest revision,
            RenderPacket packet,
            Map<String, Object> stateOut,
            Map<String, Object> stateIn,
            List<Ids.BlockId> blockIds,
            List<Ids.SceneId> sceneIds,
            List<DetectorFinding> findings
    ) {
        AdjacentMetadataDetectorAddBoundaryFieldFinding.addBoundaryFieldFinding(
                revision, packet, "location", FindingCode.LOCATION_CHANGED_WITHOUT_TRANSITION,
                stateOut, stateIn, blockIds, sceneIds, findings
        );
        AdjacentMetadataDetectorAddBoundaryFieldFinding.addBoundaryFieldFinding(
                revision, packet, "weather", FindingCode.WEATHER_CHANGED_WITHOUT_EVIDENCE,
                stateOut, stateIn, blockIds, sceneIds, findings
        );
        AdjacentMetadataDetectorAddBoundaryFieldFinding.addBoundaryFieldFinding(
                revision, packet, "time", FindingCode.TIME_DISCONTINUITY,
                stateOut, stateIn, blockIds, sceneIds, findings
        );
        AdjacentMetadataDetectorAddBoundaryFieldFinding.addBoundaryFieldFinding(
                revision, packet, "pov", FindingCode.POV_CHANGED_WITHOUT_BOUNDARY,
                stateOut, stateIn, blockIds, sceneIds, findings
        );

        Set<String> before = AdjacentMetadataDetectorStrings.strings(stateOut.get("present_character_ids"));
        Set<String> after = AdjacentMetadataDetectorStrings.strings(stateIn.get("present_character_ids"));
        for (String characterId : AdjacentMetadataDetectorDifference.difference(after, before)) {
            AdjacentMetadataDetectorAddBoundaryCharacterFinding.addBoundaryCharacterFinding(
                    revision,
                    packet,
                    FindingCode.CHARACTER_APPEARED_WITHOUT_ENTER,
                    characterId,
                    blockIds,
                    sceneIds,
                    findings
            );
        }
        for (String characterId : AdjacentMetadataDetectorDifference.difference(before, after)) {
            AdjacentMetadataDetectorAddBoundaryCharacterFinding.addBoundaryCharacterFinding(
                    revision,
                    packet,
                    FindingCode.CHARACTER_DISAPPEARED_WITHOUT_EXIT,
                    characterId,
                    blockIds,
                    sceneIds,
                    findings
            );
        }
    }
}
