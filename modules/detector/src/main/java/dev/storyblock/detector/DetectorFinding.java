package dev.storyblock.detector;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.StableIds;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

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
        affectedBlockIds = distinctCopy(affectedBlockIds, "affectedBlockIds");
        affectedSceneIds = distinctCopy(affectedSceneIds, "affectedSceneIds");
        contextBlockIds = distinctCopy(contextBlockIds, "contextBlockIds");
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

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("finding_id", findingId.value());
        value.put("code", code.name());
        value.put("severity", severity.canonicalName());
        value.put("revision_id", revisionId.value());
        value.put("revision_hash", revisionHash);
        value.put("rule_version", ruleVersion);
        value.put(
                "affected_block_ids",
                affectedBlockIds.stream().map(Ids.BlockId::value).toList()
        );
        value.put(
                "affected_scene_ids",
                affectedSceneIds.stream().map(Ids.SceneId::value).toList()
        );
        value.put(
                "context_block_ids",
                contextBlockIds.stream().map(Ids.BlockId::value).toList()
        );
        value.put("evidence", evidence);
        return CanonicalValues.freezeMap(value, "detector_finding");
    }

    private static <T> List<T> distinctCopy(List<T> values, String label) {
        List<T> copy = List.copyOf(values);
        if (new HashSet<>(copy).size() != copy.size()) {
            throw new IllegalArgumentException(label + " cannot contain duplicates");
        }
        return copy;
    }
}
