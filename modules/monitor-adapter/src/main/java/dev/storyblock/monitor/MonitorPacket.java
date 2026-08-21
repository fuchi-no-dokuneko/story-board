package dev.storyblock.monitor;

import dev.storyblock.detector.DetectorFinding;
import dev.storyblock.domain.CanonicalValues;
import dev.storyblock.domain.Ids;
import dev.storyblock.renderer.RenderPacket;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public record MonitorPacket(
        Ids.NovelId novelId,
        Ids.RevisionId revisionId,
        String revisionHash,
        String monitorVersion,
        String ruleVersion,
        String detectorRuleVersion,
        Ids.BlockId targetBlockId,
        int neighborCount,
        RenderPacket renderPacket,
        List<DetectorFinding> detectorFindings,
        MonitorLocalInvariants localInvariants,
        List<MonitorTool> allowedTools
) {
    private static final Pattern HASH = Pattern.compile("sha256:[0-9a-f]{64}");

    public MonitorPacket {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        if (revisionHash == null || !HASH.matcher(revisionHash).matches()) {
            throw new IllegalArgumentException("Monitor packet hash must be lowercase SHA-256");
        }
        if (monitorVersion == null || monitorVersion.isBlank()
                || ruleVersion == null || ruleVersion.isBlank()
                || detectorRuleVersion == null || detectorRuleVersion.isBlank()) {
            throw new IllegalArgumentException("Monitor packet versions cannot be blank");
        }
        Objects.requireNonNull(targetBlockId, "targetBlockId");
        if (neighborCount < 1 || neighborCount > MonitorModule.MAX_NEIGHBORS) {
            throw new IllegalArgumentException("Monitor neighbor count must be 1 or 2");
        }
        Objects.requireNonNull(renderPacket, "renderPacket");
        detectorFindings = List.copyOf(detectorFindings);
        Objects.requireNonNull(localInvariants, "localInvariants");
        allowedTools = List.copyOf(allowedTools);
        if (!allowedTools.equals(List.of(
                MonitorTool.SUBMIT_FINDING,
                MonitorTool.SUBMIT_PROPOSED_OPERATION
        ))) {
            throw new IllegalArgumentException("Monitor packet has unsupported tools");
        }
        if (!novelId.equals(renderPacket.novelId())
                || !revisionId.equals(renderPacket.revisionId())
                || !revisionHash.equals(renderPacket.revisionHash())
                || renderPacket.blocks().stream()
                        .noneMatch(block -> block.blockId().equals(targetBlockId))) {
            throw new IllegalArgumentException("Monitor packet render identity is inconsistent");
        }
        List<Ids.BlockId> renderedIds = renderPacket.blocks().stream()
                .map(block -> block.blockId()).toList();
        List<Ids.BlockId> fingerprintIds = localInvariants.windowBlocks().stream()
                .map(MonitorBlockFingerprint::blockId).toList();
        if (!renderedIds.equals(fingerprintIds)) {
            throw new IllegalArgumentException(
                    "Monitor render blocks and fingerprints must align"
            );
        }
        MonitorBlockFingerprint targetFingerprint = localInvariants.windowBlocks().stream()
                .filter(block -> block.blockId().equals(targetBlockId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Monitor invariants do not contain the target block"
                ));
        if (!targetFingerprint.blockVersionId().equals(
                localInvariants.targetBlockVersionId()
        )) {
            throw new IllegalArgumentException(
                    "Monitor target block version does not match its fingerprint"
            );
        }
        Set<Ids.BlockId> windowIds = Set.copyOf(renderedIds);
        for (DetectorFinding finding : detectorFindings) {
            if (!windowIds.containsAll(finding.affectedBlockIds())
                    || !windowIds.containsAll(finding.contextBlockIds())) {
                throw new IllegalArgumentException(
                        "Monitor detector findings must remain inside the packet window"
                );
            }
        }
    }

    public Map<String, Object> canonicalValue() {
        return CanonicalValues.freezeMap(Map.ofEntries(
                Map.entry("allowed_tools", allowedTools.stream()
                        .map(MonitorTool::canonicalName).toList()),
                Map.entry("detector_findings", detectorFindings.stream()
                        .map(DetectorFinding::canonicalValue).toList()),
                Map.entry("detector_rule_version", detectorRuleVersion),
                Map.entry("local_invariants", localInvariants.canonicalValue()),
                Map.entry("monitor_version", monitorVersion),
                Map.entry("neighbor_count", neighborCount),
                Map.entry("novel_id", novelId.value()),
                Map.entry("render_packet", renderPacket.canonicalValue()),
                Map.entry("revision_hash", revisionHash),
                Map.entry("revision_id", revisionId.value()),
                Map.entry("rule_version", ruleVersion),
                Map.entry("target_block_id", targetBlockId.value())
        ), "monitor_packet");
    }
}
