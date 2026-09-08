package dev.storyblock.application;

import dev.storyblock.domain.Ids;
import dev.storyblock.domain.NarrativeBlock;
import dev.storyblock.monitor.MonitorBlockFingerprint;
import dev.storyblock.monitor.MonitorRunStatus;
import dev.storyblock.monitor.MonitorStaleReason;
import dev.storyblock.monitor.StoredMonitorRun;
import dev.storyblock.storage.RevisionRef;
import dev.storyblock.storage.StoredRevision;
import java.util.EnumSet;
import java.util.Map;

final class MonitorServiceStatusAction {
    static MonitorRunStatus status(MonitorService self, StoredMonitorRun run)  {
        RevisionRef head = self.revisions.getHead(run.novelId());
        StoredRevision current = self.revisions.getRevision(
                run.novelId(), head.revisionId()
        );
        EnumSet<MonitorStaleReason> reasons = EnumSet.noneOf(MonitorStaleReason.class);
        if (!head.revisionId().equals(run.revisionId())
                || !head.contentHash().equals(run.revisionHash())) {
            reasons.add(MonitorStaleReason.HEAD_CHANGED);
        }
        if (!self.currentRuleVersion.equals(run.ruleVersion())) {
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
}
