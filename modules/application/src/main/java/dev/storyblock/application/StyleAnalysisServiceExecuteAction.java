package dev.storyblock.application;

import dev.storyblock.style.StyleAnalysisCompletionCommand;
import dev.storyblock.style.StyleAnalysisCompletionResult;
import dev.storyblock.style.StyleAnalysisExecution;
import dev.storyblock.style.StyleAnalysisLease;
import dev.storyblock.style.StyleAnalysisTrace;
import java.time.Instant;
import java.util.Objects;

final class StyleAnalysisServiceExecuteAction {
    static StyleAnalysisCompletionResult execute(StyleAnalysisService self, StyleAnalysisLease lease, String idempotencyKey, Instant completedAt)  {
        Objects.requireNonNull(lease, "lease");
        StyleAnalysisExecution execution = self.executor.execute(lease.snapshot());
        StyleAnalysisTrace trace = StyleAnalysisTrace.create(
                lease.analysisId(),
                execution.tracePayload(),
                completedAt,
                lease.retentionUntil()
        );
        return self.complete(new StyleAnalysisCompletionCommand(
                lease.jobId(),
                lease.leaseOwner(),
                lease.attempt(),
                lease.claimedStatusHash(),
                lease.snapshot().snapshotHash(),
                lease.snapshot().profileVersionHash(),
                lease.snapshot().analyzerContractHash(),
                lease.snapshot().windowConfigurationHash(),
                execution.summary(),
                execution.windows(),
                trace,
                idempotencyKey,
                completedAt
        ));
    }
}
