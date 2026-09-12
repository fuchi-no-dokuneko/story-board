package dev.storyblock.storage.sqlite;

import dev.storyblock.style.StyleAnalysisClaimCommand;
import dev.storyblock.style.StyleAnalysisJob;
import dev.storyblock.style.StyleAnalysisLease;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;

final class SqliteAnalysisClaim {
    static Optional<StyleAnalysisLease> claim(
            Connection connection,
            StyleAnalysisClaimCommand command
    ) throws SQLException {
        Optional<ClaimReceipt> prior = SqliteAnalysisFindClaimReceipt.findClaimReceipt(
                connection, command.novelId(), command.idempotencyKey()
        );
        if (prior.isPresent()) {
            ClaimReceipt receipt = prior.get();
            return SqliteAnalysisReplayLease.replay(receipt, command);
        }

        SqliteAnalysisFailExhaustedLeases.failExhaustedLeases(connection, command.claimedAt());
        Optional<StyleAnalysisJob> candidate = SqliteAnalysisFindClaimCandidate.findClaimCandidate(
                connection, command.novelId(), command.claimedAt()
        );
        if (candidate.isEmpty()) {
            SqliteAnalysisInsertClaimReceipt.insertClaimReceipt(connection, command, null);
            return Optional.empty();
        }

        StyleAnalysisJob current = candidate.get();
        Instant leaseUntil = command.claimedAt().plus(command.leaseDuration());
        int attempt = current.attempt() + 1;
        SqliteAnalysisFenceLease.update(connection, current, command, leaseUntil, attempt);
        StyleAnalysisJob claimed = SqliteAnalysisGetJob.getJob(connection, current.jobId());
        StyleAnalysisLease lease = new StyleAnalysisLease(
                claimed.jobId(),
                claimed.analysisId(),
                claimed.snapshot(),
                command.leaseOwner(),
                attempt,
                leaseUntil,
                claimed.retentionUntil(),
                claimed.statusHash(),
                false
        );
        SqliteAnalysisInsertClaimReceipt.insertClaimReceipt(connection, command, lease);
        return Optional.of(lease);
    }
}
