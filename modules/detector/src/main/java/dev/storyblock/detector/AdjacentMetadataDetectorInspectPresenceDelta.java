package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Set;
import static dev.storyblock.detector.AdjacentMetadataDetector.BlockContext;

final class AdjacentMetadataDetectorInspectPresenceDelta {
    static void inspectPresenceDelta(
            RevisionManifest revision,
            RenderPacket packet,
            BlockContext current,
            List<Ids.BlockId> contextIds,
            List<DetectorFinding> findings
    ) {
        Set<String> before = AdjacentMetadataDetectorStrings.strings(current.resolved().before().get("present_character_ids"));
        Set<String> after = AdjacentMetadataDetectorStrings.strings(current.resolved().after().get("present_character_ids"));
        Set<String> entered = AdjacentMetadataDetectorEventCharacters.eventCharacters(current.resolved().events(), "enter");
        Set<String> exited = AdjacentMetadataDetectorEventCharacters.eventCharacters(current.resolved().events(), "exit");

        for (String characterId : AdjacentMetadataDetectorDifference.difference(after, before)) {
            if (!entered.contains(characterId)) {
                AdjacentMetadataDetectorAddCharacterFinding.addCharacterFinding(
                        revision,
                        packet,
                        current,
                        contextIds,
                        FindingCode.CHARACTER_APPEARED_WITHOUT_ENTER,
                        characterId,
                        "block_transition",
                        findings
                );
            }
        }
        for (String characterId : AdjacentMetadataDetectorDifference.difference(before, after)) {
            if (!exited.contains(characterId)) {
                AdjacentMetadataDetectorAddCharacterFinding.addCharacterFinding(
                        revision,
                        packet,
                        current,
                        contextIds,
                        FindingCode.CHARACTER_DISAPPEARED_WITHOUT_EXIT,
                        characterId,
                        "block_transition",
                        findings
                );
            }
        }
    }
}
