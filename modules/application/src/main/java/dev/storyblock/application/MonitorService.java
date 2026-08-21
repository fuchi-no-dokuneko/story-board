package dev.storyblock.application;

import dev.storyblock.domain.BlockRangeGuard;
import dev.storyblock.domain.EditOperation;
import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.domain.NarrativeChapter;
import dev.storyblock.domain.NarrativeScene;
import dev.storyblock.domain.RevisionManifest;
import dev.storyblock.domain.SceneBoundaryContract;
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
import java.util.HashSet;
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
        validateEvidence(packet, affected, output);
        if (output instanceof MonitorProposedOperation proposal) {
            validateProposal(packet, affected, proposal.operation());
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

        Map<Ids.BlockId, NarrativeBlock> currentBlocks = blocks(current.manifest());
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

    private static void validateEvidence(
            MonitorPacket packet,
            Set<Ids.BlockId> affected,
            MonitorOutput output
    ) {
        Map<Ids.BlockId, String> textByBlock = new LinkedHashMap<>();
        packet.renderPacket().blocks().forEach(block ->
                textByBlock.put(block.blockId(), block.text())
        );
        Set<Ids.BlockId> evidenced = new HashSet<>();
        output.evidence().forEach(evidence -> {
            String text = textByBlock.get(evidence.blockId());
            if (text == null || !evidence.matches(text)) {
                throw new IllegalArgumentException(
                        "Monitor evidence must match source text inside the packet window"
                );
            }
            evidenced.add(evidence.blockId());
        });
        if (!evidenced.equals(affected)) {
            throw new IllegalArgumentException(
                    "Every affected monitor block must have text evidence"
            );
        }
    }

    private static void validateProposal(
            MonitorPacket packet,
            Set<Ids.BlockId> affected,
            EditOperation operation
    ) {
        if (!operation.context().novelId().equals(packet.novelId())
                || !operation.context().baseRevisionId().equals(packet.revisionId())
                || !operation.context().expectedHeadHash().equals(packet.revisionHash())) {
            throw new IllegalArgumentException(
                    "Monitor proposal context must match the source packet"
            );
        }
        if (operation instanceof EditOperation.RestoreRevisionContent) {
            throw new IllegalArgumentException(
                    "Monitor proposals cannot restore global revision content"
            );
        }
        Set<Ids.BlockId> window = packet.localInvariants().windowBlocks().stream()
                .map(MonitorBlockFingerprint::blockId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Ids.BlockId> references = referencedBlockIds(operation);
        if (!window.containsAll(references)) {
            throw new IllegalArgumentException(
                    "Monitor proposal block references must remain inside the packet window"
            );
        }
        Set<Ids.SceneId> windowScenes = packet.renderPacket().sceneBoundaries().stream()
                .map(boundary -> boundary.sceneId())
                .collect(java.util.stream.Collectors.toSet());
        if (!windowScenes.containsAll(referencedSceneIds(operation))) {
            throw new IllegalArgumentException(
                    "Monitor proposal scene references must remain inside the packet window"
            );
        }
        Set<Ids.BlockId> directlyChanged = directlyChangedBlockIds(operation);
        if (!affected.containsAll(directlyChanged)) {
            throw new IllegalArgumentException(
                    "Monitor proposal affected IDs must include every changed source block"
            );
        }
    }

    private static Set<Ids.BlockId> referencedBlockIds(EditOperation operation) {
        Set<Ids.BlockId> result = new LinkedHashSet<>();
        if (operation instanceof EditOperation.InsertBlocks value) {
            add(result, value.insertionPoint().anchorBlockId());
        } else if (operation instanceof EditOperation.ReplaceBlockRange value) {
            addRange(result, value.range());
        } else if (operation instanceof EditOperation.DeleteBlockRange value) {
            addRange(result, value.range());
        } else if (operation instanceof EditOperation.SplitBlock value) {
            addRange(result, value.block());
        } else if (operation instanceof EditOperation.MergeBlocks value) {
            addRange(result, value.range());
        } else if (operation instanceof EditOperation.ExtendBlock value) {
            addRange(result, value.block());
        } else if (operation instanceof EditOperation.MoveBlockRange value) {
            addRange(result, value.range());
            add(result, value.destination().anchorBlockId());
            addBoundary(result, value.expectedSourceBoundary());
            addBoundary(result, value.expectedDestinationBoundary());
        } else if (operation instanceof EditOperation.CorrectBlockMeta value) {
            result.add(value.block().blockId());
        } else if (operation instanceof EditOperation.SetSceneInitialMeta value) {
            addBoundary(result, value.expectedBoundary());
        }
        return Set.copyOf(result);
    }

    private static Set<Ids.BlockId> directlyChangedBlockIds(EditOperation operation) {
        if (operation instanceof EditOperation.ReplaceBlockRange value) {
            return rangeIds(value.range());
        }
        if (operation instanceof EditOperation.DeleteBlockRange value) {
            return rangeIds(value.range());
        }
        if (operation instanceof EditOperation.SplitBlock value) {
            return rangeIds(value.block());
        }
        if (operation instanceof EditOperation.MergeBlocks value) {
            return rangeIds(value.range());
        }
        if (operation instanceof EditOperation.ExtendBlock value) {
            return rangeIds(value.block());
        }
        if (operation instanceof EditOperation.MoveBlockRange value) {
            return rangeIds(value.range());
        }
        if (operation instanceof EditOperation.CorrectBlockMeta value) {
            return Set.of(value.block().blockId());
        }
        return Set.of();
    }

    private static Set<Ids.SceneId> referencedSceneIds(EditOperation operation) {
        Set<Ids.SceneId> result = new LinkedHashSet<>();
        if (operation instanceof EditOperation.InsertBlocks value) {
            result.add(value.insertionPoint().sceneId());
        } else if (operation instanceof EditOperation.ReplaceBlockRange value) {
            result.add(value.range().sceneId());
        } else if (operation instanceof EditOperation.DeleteBlockRange value) {
            result.add(value.range().sceneId());
        } else if (operation instanceof EditOperation.SplitBlock value) {
            result.add(value.block().sceneId());
        } else if (operation instanceof EditOperation.MergeBlocks value) {
            result.add(value.range().sceneId());
        } else if (operation instanceof EditOperation.ExtendBlock value) {
            result.add(value.block().sceneId());
        } else if (operation instanceof EditOperation.MoveBlockRange value) {
            result.add(value.range().sceneId());
            result.add(value.destination().sceneId());
            result.add(value.expectedSourceBoundary().sceneId());
            result.add(value.expectedDestinationBoundary().sceneId());
        } else if (operation instanceof EditOperation.CorrectBlockMeta value) {
            result.add(value.sceneId());
        } else if (operation instanceof EditOperation.SetSceneInitialMeta value) {
            result.add(value.sceneId());
            result.add(value.expectedBoundary().sceneId());
        }
        return Set.copyOf(result);
    }

    private static Set<Ids.BlockId> rangeIds(BlockRangeGuard range) {
        return range.expectedBlocks().stream()
                .map(reference -> reference.blockId())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static void addRange(Set<Ids.BlockId> result, BlockRangeGuard range) {
        result.addAll(rangeIds(range));
        add(result, range.expectedPreviousBlockId());
        add(result, range.expectedNextBlockId());
    }

    private static void addBoundary(
            Set<Ids.BlockId> result,
            SceneBoundaryContract boundary
    ) {
        add(result, boundary.firstBlockId());
        add(result, boundary.lastBlockId());
    }

    private static void add(Set<Ids.BlockId> result, Ids.BlockId blockId) {
        if (blockId != null) {
            result.add(blockId);
        }
    }

    private static Map<Ids.BlockId, NarrativeBlock> blocks(RevisionManifest revision) {
        Map<Ids.BlockId, NarrativeBlock> result = new LinkedHashMap<>();
        for (NarrativeChapter chapter : revision.novel().chapters()) {
            for (NarrativeScene scene : chapter.scenes()) {
                for (NarrativeBlock block : scene.blocks()) {
                    result.put(block.id(), block);
                }
            }
        }
        return Map.copyOf(result);
    }
}
