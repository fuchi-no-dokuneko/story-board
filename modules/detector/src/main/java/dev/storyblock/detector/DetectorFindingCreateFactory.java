package dev.storyblock.detector;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.StableIds;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DetectorFindingCreateFactory {
    static DetectorFinding create(FindingCode code, Ids.RevisionId revisionId, String revisionHash, List<Ids.BlockId> affectedBlockIds, List<Ids.SceneId> affectedSceneIds, List<Ids.BlockId> contextBlockIds, Map<String, Object> evidence)  {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("code", code.name());
        identity.put("revision_hash", revisionHash);
        identity.put("rule_version", DetectorModule.VERSION);
        identity.put(
                "affected_block_ids",
                affectedBlockIds.stream().map(Ids.BlockId::value).toList()
        );
        identity.put(
                "affected_scene_ids",
                affectedSceneIds.stream().map(Ids.SceneId::value).toList()
        );
        identity.put(
                "context_block_ids",
                contextBlockIds.stream().map(Ids.BlockId::value).toList()
        );
        identity.put("evidence", evidence);
        String discriminator = CanonicalJson.hash(identity);
        Ids.FindingId findingId = new Ids.FindingId(
                StableIds.derive("fnd", revisionId.value(), discriminator)
        );
        return new DetectorFinding(
                findingId,
                code,
                code.defaultSeverity(),
                revisionId,
                revisionHash,
                DetectorModule.VERSION,
                affectedBlockIds,
                affectedSceneIds,
                contextBlockIds,
                evidence
        );
    }
}
