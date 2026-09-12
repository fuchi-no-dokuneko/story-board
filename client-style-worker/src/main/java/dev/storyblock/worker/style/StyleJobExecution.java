package dev.storyblock.worker.style;
import dev.storyblock.style.*;
import java.time.Instant;

final class StyleJobExecution {
  static StyleAnalysisCompletionCommand execute(StyleWorkerClient self, StyleAnalysisLease lease) {
    StyleAnalysisExecution execution = self.executor.execute(lease.snapshot());
    Instant completedAt = Instant.now(self.clock);
    if (!completedAt.isBefore(lease.leaseUntil())) {
      throw new StyleWorkerProtocolException(
          "Style job lease expired before execution completed"
      );
    }
    StyleAnalysisTrace trace = StyleAnalysisTrace.create(
        lease.analysisId(),
        execution.tracePayload(),
        completedAt,
        lease.retentionUntil()
    );
    String resultKey = "style-result-" + lease.jobId().value()
        + "-" + lease.attempt();
    StyleAnalysisCompletionCommand completion = new StyleAnalysisCompletionCommand(
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
        resultKey,
        completedAt
    );
    return completion;
  }
}
