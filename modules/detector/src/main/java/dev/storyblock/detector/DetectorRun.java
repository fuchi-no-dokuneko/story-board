package dev.storyblock.detector;

import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record DetectorRun(
        Ids.RevisionId revisionId,
        String revisionHash,
        String ruleVersion,
        List<DetectorFinding> findings
) {
    private static final Pattern SHA_256 = Pattern.compile("sha256:[0-9a-f]{64}");

    public DetectorRun {
        Objects.requireNonNull(revisionId, "revisionId");
        if (revisionHash == null || !SHA_256.matcher(revisionHash).matches()) {
            throw new IllegalArgumentException("Detector revision hash must be lowercase SHA-256");
        }
        if (ruleVersion == null || ruleVersion.isBlank()) {
            throw new IllegalArgumentException("Detector rule version cannot be blank");
        }
        findings = List.copyOf(findings);
        for (DetectorFinding finding : findings) {
            if (!revisionId.equals(finding.revisionId())
                    || !revisionHash.equals(finding.revisionHash())
                    || !ruleVersion.equals(finding.ruleVersion())) {
                throw new IllegalArgumentException(
                        "Detector findings must match their run revision and rule version"
                );
            }
        }
    }

    public Map<String, Object> canonicalValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("revision_id", revisionId.value());
        value.put("revision_hash", revisionHash);
        value.put("rule_version", ruleVersion);
        value.put(
                "findings",
                findings.stream().map(DetectorFinding::canonicalValue).toList()
        );
        return CanonicalValues.freezeMap(value, "detector_run");
    }
}
