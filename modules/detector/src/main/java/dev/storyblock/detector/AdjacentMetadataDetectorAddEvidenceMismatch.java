package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;
import static dev.storyblock.detector.AdjacentMetadataDetector.BlockContext;

final class AdjacentMetadataDetectorAddEvidenceMismatch {
    static void addEvidenceMismatch(
            RevisionManifest revision,
            RenderPacket packet,
            BlockContext current,
            List<Ids.BlockId> contextIds,
            String path,
            List<DetectorFinding> findings
    ) {
        AdjacentMetadataDetectorAddFinding.addFinding(
                revision,
                packet,
                FindingCode.META_TEXT_MISMATCH,
                List.of(current.blockId()),
                List.of(current.scene().id()),
                contextIds,
                Map.of(
                        "evidence_path", path,
                        "kind", "invalid_metadata_evidence",
                        "reason", "stale_or_invalid_span"
                ),
                findings
        );
    }
}
