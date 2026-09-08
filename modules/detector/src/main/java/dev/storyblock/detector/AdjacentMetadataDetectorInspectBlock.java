package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;
import static dev.storyblock.detector.AdjacentMetadataDetector.BlockContext;

final class AdjacentMetadataDetectorInspectBlock {
    static void inspectBlock(
            RevisionManifest revision,
            RenderPacket packet,
            List<BlockContext> blocks,
            int index,
            List<DetectorFinding> findings
    ) {
        BlockContext current = blocks.get(index);
        Map<String, Object> local = current.rendered().localMetadata().fields();
        List<Ids.BlockId> contextIds = AdjacentMetadataDetectorContextBlockIds.contextBlockIds(blocks, index);

        AdjacentMetadataDetectorInspectClaimedEvidence.inspectClaimedEvidence(revision, packet, current, contextIds, findings);
        AdjacentMetadataDetectorInspectPresenceDelta.inspectPresenceDelta(revision, packet, current, contextIds, findings);

        boolean transitionBlock = "transition".equals(local.get("narrative_mode"));
        AdjacentMetadataDetectorInspectFieldChange.inspectFieldChange(
                revision,
                packet,
                current,
                contextIds,
                "location",
                FindingCode.LOCATION_CHANGED_WITHOUT_TRANSITION,
                !transitionBlock && !AdjacentMetadataDetectorHasValidEvidence.hasValidEvidence(local.get("location"), local, current.text()),
                findings
        );
        AdjacentMetadataDetectorInspectFieldChange.inspectFieldChange(
                revision,
                packet,
                current,
                contextIds,
                "weather",
                FindingCode.WEATHER_CHANGED_WITHOUT_EVIDENCE,
                !AdjacentMetadataDetectorHasValidEvidence.hasValidEvidence(local.get("weather"), local, current.text()),
                findings
        );
        AdjacentMetadataDetectorInspectFieldChange.inspectFieldChange(
                revision,
                packet,
                current,
                contextIds,
                "time",
                FindingCode.TIME_DISCONTINUITY,
                !transitionBlock && !AdjacentMetadataDetectorHasValidEvidence.hasValidEvidence(local.get("time"), local, current.text()),
                findings
        );
        AdjacentMetadataDetectorInspectFieldChange.inspectFieldChange(
                revision,
                packet,
                current,
                contextIds,
                "pov",
                FindingCode.POV_CHANGED_WITHOUT_BOUNDARY,
                !current.firstInScene() && !transitionBlock,
                findings
        );
    }
}
