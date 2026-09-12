package dev.storyblock.detector;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record DetectorFinding(
        Ids.FindingId findingId,
        FindingCode code,
        FindingSeverity severity,
        Ids.RevisionId revisionId,
        String revisionHash,
        String ruleVersion,
        List<Ids.BlockId> affectedBlockIds,
        List<Ids.SceneId> affectedSceneIds,
        List<Ids.BlockId> contextBlockIds,
        Map<String, Object> evidence
) {
    static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public DetectorFinding {
        Objects.requireNonNull(findingId, "findingId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(revisionId, "revisionId");
        if (revisionHash == null || !SHA_256.matcher(revisionHash).matches()) {
            throw new IllegalArgumentException("Finding revision hash must be lowercase SHA-256");
        }
        if (ruleVersion == null || ruleVersion.isBlank()) {
            throw new IllegalArgumentException("Finding rule version cannot be blank");
        }
        affectedBlockIds = DetectorFindingDistinctCopy.distinctCopy(affectedBlockIds, "affectedBlockIds");
        affectedSceneIds = DetectorFindingDistinctCopy.distinctCopy(affectedSceneIds, "affectedSceneIds");
        contextBlockIds = DetectorFindingDistinctCopy.distinctCopy(contextBlockIds, "contextBlockIds");
        if (affectedBlockIds.isEmpty() && affectedSceneIds.isEmpty()) {
            throw new IllegalArgumentException("A detector finding must identify an affected object");
        }
        evidence = CanonicalValues.freezeMap(evidence, "detector_finding.evidence");
    }

    public static DetectorFinding create(
            FindingCode code,
            Ids.RevisionId revisionId,
            String revisionHash,
            List<Ids.BlockId> affectedBlockIds,
            List<Ids.SceneId> affectedSceneIds,
            List<Ids.BlockId> contextBlockIds,
            Map<String, Object> evidence
    ) {
        return DetectorFindingCreateFactory.create(code, revisionId, revisionHash, affectedBlockIds, affectedSceneIds, contextBlockIds, evidence);
    }

    public Map<String, Object> canonicalValue() {
        return DetectorFindingCanonicalValueAction.canonicalValue(this);
    }

}
