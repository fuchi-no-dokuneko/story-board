package dev.storyblock.storage.sqlite;

import dev.storyblock.style.*;

final class SqliteAnalysisRequireCompletionVersions {
    static void requireCompletionVersions(
            StyleAnalysisJob job,
            StyleAnalysisCompletionCommand command
    ) {
        StyleAnalysisSnapshot snapshot = job.snapshot();
        if (!snapshot.snapshotHash().equals(command.snapshotHash())
                || !snapshot.profileVersionHash().equals(command.profileVersionHash())
                || !snapshot.analyzerContractHash().equals(
                        command.analyzerContractHash()
                )
                || !snapshot.windowConfigurationHash().equals(
                        command.windowConfigurationHash()
                )
                || !job.analysisId().equals(command.trace().analysisId())
                || !job.retentionUntil().equals(command.trace().expiresAt())) {
            throw new StyleAnalysisLeaseConflictException(
                    "Style analysis result versions do not match the leased snapshot",
                    job.statusHash()
            );
        }
    }
}
