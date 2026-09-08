package dev.storyblock.storage.sqlite;
import dev.storyblock.storage.IdempotencyConflictException;
import dev.storyblock.style.*;
import java.util.Optional;
import static dev.storyblock.storage.sqlite.SqliteAnalysisData.*;
final class SqliteAnalysisReplayLease {
    static Optional<StyleAnalysisLease> replay(ClaimReceipt receipt, StyleAnalysisClaimCommand command) {
        if (!receipt.requestHash().equals(command.requestHash())) {
            throw new IdempotencyConflictException(
                    command.idempotencyKey(),
                    receipt.requestHash(),
                    command.requestHash()
            );
        }
        return receipt.jobId() == null
                ? Optional.empty()
                : Optional.of(new StyleAnalysisLease(
                        receipt.jobId(),
                        receipt.analysisId(),
                        receipt.snapshot(),
                        receipt.leaseOwner(),
                        receipt.attempt(),
                        receipt.leaseUntil(),
                        receipt.retentionUntil(),
                        receipt.claimedStatusHash(),
                        true
                ));
    }
}
