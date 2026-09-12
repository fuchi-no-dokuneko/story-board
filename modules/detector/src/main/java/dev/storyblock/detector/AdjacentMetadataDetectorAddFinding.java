package dev.storyblock.detector;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;

final class AdjacentMetadataDetectorAddFinding {
    static void addFinding(
            RevisionManifest revision,
            RenderPacket packet,
            FindingCode code,
            List<Ids.BlockId> affectedBlockIds,
            List<Ids.SceneId> affectedSceneIds,
            List<Ids.BlockId> contextBlockIds,
            Map<String, Object> evidence,
            List<DetectorFinding> findings
    ) {
        findings.add(DetectorFinding.create(
                code,
                revision.id(),
                packet.revisionHash(),
                affectedBlockIds,
                affectedSceneIds,
                contextBlockIds,
                evidence
        ));
    }
}
