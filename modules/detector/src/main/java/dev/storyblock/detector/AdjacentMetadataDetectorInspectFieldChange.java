package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;
import static dev.storyblock.detector.AdjacentMetadataDetector.BlockContext;

final class AdjacentMetadataDetectorInspectFieldChange {
    static void inspectFieldChange(
            RevisionManifest revision,
            RenderPacket packet,
            BlockContext current,
            List<Ids.BlockId> contextIds,
            String field,
            FindingCode code,
            boolean report,
            List<DetectorFinding> findings
    ) {
        Object before = current.resolved().before().get(field);
        Object after = current.resolved().after().get(field);
        Object local = current.rendered().localMetadata().fields().get(field);
        if (!report || !AdjacentMetadataDetectorIsExplicit.isExplicit(local) || !AdjacentMetadataDetectorComparableChange.comparableChange(before, after)) {
            return;
        }

        AdjacentMetadataDetectorAddFinding.addFinding(
                revision,
                packet,
                code,
                List.of(current.blockId()),
                List.of(current.scene().id()),
                contextIds,
                Map.of(
                        "after", after,
                        "before", before,
                        "field", field,
                        "kind", "block_transition"
                ),
                findings
        );
    }
}
