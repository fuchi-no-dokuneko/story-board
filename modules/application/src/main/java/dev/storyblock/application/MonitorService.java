package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.monitor.MonitorBlockFingerprint;
import dev.storyblock.monitor.MonitorModule;
import dev.storyblock.monitor.MonitorOutput;
import dev.storyblock.monitor.MonitorOutputKind;
import dev.storyblock.monitor.MonitorPacket;
import dev.storyblock.monitor.MonitorPacketFactory;
import dev.storyblock.monitor.MonitorProposedOperation;
import dev.storyblock.monitor.MonitorRunStatus;
import dev.storyblock.monitor.MonitorSaveResult;
import dev.storyblock.monitor.MonitorStaleReason;
import dev.storyblock.monitor.MonitorStore;
import dev.storyblock.monitor.MonitorSubmissionResult;
import dev.storyblock.monitor.StoredMonitorRun;
import dev.storyblock.security.AuditContext;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.RevisionStore;
import dev.storyblock.storage.StaleHeadException;
import dev.storyblock.storage.StoredRevision;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MonitorService {
    private final RevisionStore revisions;
    private final MonitorStore monitors;
    private final MonitorPacketFactory packets;
    private final String currentRuleVersion;

    public MonitorService(RevisionStore revisions, MonitorStore monitors) {
        this(revisions, monitors, new MonitorPacketFactory(), MonitorModule.RULE_VERSION);
    }

    MonitorService(
            RevisionStore revisions,
            MonitorStore monitors,
            MonitorPacketFactory packets,
            String currentRuleVersion
    ) {
        this.revisions = Objects.requireNonNull(revisions, "revisions");
        this.monitors = Objects.requireNonNull(monitors, "monitors");
        this.packets = Objects.requireNonNull(packets, "packets");
        if (currentRuleVersion == null || currentRuleVersion.isBlank()) {
            throw new IllegalArgumentException("Current monitor rule version cannot be blank");
        }
        this.currentRuleVersion = currentRuleVersion;
    }

    public MonitorPacket packet(
            Ids.NovelId novelId,
            Ids.RevisionId revisionId,
            String revisionHash,
            Ids.BlockId targetBlockId,
            int neighborCount
    ) {
        StoredRevision revision = requireRevision(novelId, revisionId, revisionHash);
        return packets.create(
                revision.manifest(), revision.contentHash(), targetBlockId, neighborCount
        );
    }

    public MonitorSubmissionResult submit(
            Ids.NovelId novelId,
            Ids.RevisionId revisionId,
            String revisionHash,
            Ids.BlockId targetBlockId,
            int neighborCount,
            String ruleVersion,
            List<Ids.BlockId> affectedBlockIds,
            MonitorOutput output,
            String idempotencyKey,
            AuditContext auditContext
    ) {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(auditContext, "auditContext");
        if (!currentRuleVersion.equals(ruleVersion)) {
            throw new IllegalArgumentException("Monitor submission rule version is not current");
        }
        MonitorPacket packet = packet(
                novelId, revisionId, revisionHash, targetBlockId, neighborCount
        );
        List<Ids.BlockId> requestedAffected = List.copyOf(affectedBlockIds);
        Set<Ids.BlockId> affected = new LinkedHashSet<>(requestedAffected);
        if (affected.isEmpty() || affected.size() != requestedAffected.size()
                || affected.size() > 5 || !affected.contains(targetBlockId)) {
            throw new IllegalArgumentException(
                    "Monitor affected block IDs must be unique, include the target, and number 1 to 5"
            );
        }

        Map<Ids.BlockId, MonitorBlockFingerprint> windowFingerprints = new LinkedHashMap<>();
        packet.localInvariants().windowBlocks().forEach(fingerprint ->
                windowFingerprints.put(fingerprint.blockId(), fingerprint)
        );
        if (!windowFingerprints.keySet().containsAll(affected)) {
            throw new IllegalArgumentException(
                    "Monitor affected block IDs must remain inside the supplied packet window"
            );
        }
        MonitorServiceValidateEvidence.validateEvidence(packet, affected, output);
        if (output instanceof MonitorProposedOperation proposal) {
            MonitorServiceValidateProposal.validateProposal(packet, affected, proposal.operation());
        }

        List<MonitorBlockFingerprint> affectedFingerprints = windowFingerprints.values()
                .stream()
                .filter(fingerprint -> affected.contains(fingerprint.blockId()))
                .toList();
        String requestHash = StoredMonitorRun.requestHash(
                novelId,
                revisionId,
                revisionHash,
                targetBlockId,
                neighborCount,
                MonitorModule.VERSION,
                ruleVersion,
                affectedFingerprints,
                output
        );
        Ids.MonitorOutputId outputId = output.kind() == MonitorOutputKind.FINDING
                ? Ids.MonitorIssueId.create()
                : Ids.MonitorProposalId.create();
        StoredMonitorRun candidate = new StoredMonitorRun(
                Ids.MonitorRunId.create(),
                outputId,
                novelId,
                revisionId,
                revisionHash,
                targetBlockId,
                neighborCount,
                MonitorModule.VERSION,
                ruleVersion,
                affectedFingerprints,
                output,
                idempotencyKey,
                requestHash,
                auditContext,
                auditContext.occurredAt()
        );
        MonitorSaveResult saved = monitors.saveMonitorRun(candidate);
        return new MonitorSubmissionResult(
                status(saved.run()), saved.idempotentReplay()
        );
    }

    public MonitorRunStatus getStatus(
            Ids.NovelId novelId,
            Ids.MonitorRunId runId
    ) {
        return status(monitors.getMonitorRun(novelId, runId));
    }

    private MonitorRunStatus status(StoredMonitorRun run) {
        RevisionRef head = revisions.getHead(run.novelId());
        StoredRevision current = revisions.getRevision(
                run.novelId(), head.revisionId()
        );
        EnumSet<MonitorStaleReason> reasons = EnumSet.noneOf(MonitorStaleReason.class);
        if (!head.revisionId().equals(run.revisionId())
                || !head.contentHash().equals(run.revisionHash())) {
            reasons.add(MonitorStaleReason.HEAD_CHANGED);
        }
        if (!currentRuleVersion.equals(run.ruleVersion())) {
            reasons.add(MonitorStaleReason.RULE_VERSION_CHANGED);
        }

        Map<Ids.BlockId, NarrativeBlock> currentBlocks = MonitorServiceBlocks.blocks(current.manifest());
        for (MonitorBlockFingerprint saved : run.affectedBlocks()) {
            NarrativeBlock block = currentBlocks.get(saved.blockId());
            if (block == null) {
                reasons.add(MonitorStaleReason.AFFECTED_BLOCK_MISSING);
            } else if (!saved.equals(MonitorBlockFingerprint.from(block))) {
                reasons.add(MonitorStaleReason.AFFECTED_BLOCK_CHANGED);
            }
        }
        return reasons.isEmpty()
                ? MonitorRunStatus.current(run)
                : MonitorRunStatus.stale(run, reasons);
    }

    private StoredRevision requireRevision(
            Ids.NovelId novelId,
            Ids.RevisionId revisionId,
            String revisionHash
    ) {
        Objects.requireNonNull(novelId, "novelId");
        Objects.requireNonNull(revisionId, "revisionId");
        Objects.requireNonNull(revisionHash, "revisionHash");
        StoredRevision revision = revisions.getRevision(novelId, revisionId);
        if (!revision.contentHash().equals(revisionHash)) {
            RevisionRef expected = new RevisionRef(
                    revisionId, revision.sequence(), revisionHash
            );
            throw new StaleHeadException(expected, revision.reference());
        }
        return revision;
    }

}
