package dev.storyblock.storage.sqlite;

import java.util.Map;

public record SqliteOperationalSnapshot(
        long commitWaitMillis,
        long commitTransactionMillis,
        long sqliteBusyTotal,
        long walBytes,
        long checkpointMillis,
        long queueDepth,
        long oldestJobAgeSeconds,
        long analysisDurationMillis,
        long rewriteDurationMillis,
        long staleProposalTotal,
        long artifactBytes,
        String migrationVersion,
        Map<String, Long> detectorFindings
) {
    public SqliteOperationalSnapshot {
        detectorFindings = Map.copyOf(detectorFindings);
    }
}
