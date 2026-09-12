package dev.storyblock.storage.sqlite;

import dev.storyblock.style.*;
import java.util.Objects;

final class SqliteAnalysisRequireActiveLease {
    static void requireActiveLease(
            StyleAnalysisJob job,
            StyleAnalysisCompletionCommand command
    ) {
        if (job.status() != StyleAnalysisJobStatus.RUNNING
                || !job.statusHash().equals(command.expectedStatusHash())
                || !Objects.equals(job.leaseOwner(), command.leaseOwner())
                || job.attempt() != command.attempt()
                || command.completedAt().isBefore(job.updatedAt())
                || !command.completedAt().isBefore(job.leaseUntil())) {
            throw new StyleAnalysisLeaseConflictException(
                    "Style analysis result does not own the active lease",
                    job.statusHash()
            );
        }
    }
}
