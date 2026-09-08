package dev.storyblock.worker.style;

import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.style.*;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import static dev.storyblock.worker.style.StyleWorkerClient.Outcome;

final class StyleWorkerClientRunOnceAction {
  static Outcome runOnce(StyleWorkerClient self) throws IOException, InterruptedException {
    String claimKey = "style-claim-" + self.nextKey();
    byte[] claimBody = CanonicalJson.bytes(Map.of(
        "novel_id", self.settings.novelId().value(),
        "lease_owner", self.settings.workerId(),
        "lease_seconds", self.settings.leaseDuration().toSeconds()
    ));
    HttpResponse<byte[]> claim = self.send(OptionalAuthorization.request(
            self.settings.endpoint("v1/internal/jobs/claims"), self.settings.bearerToken()
        )
        .timeout(StyleWorkerClient.REQUEST_TIMEOUT)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Idempotency-Key", claimKey)
        .header("If-Match", "*")
        .POST(HttpRequest.BodyPublishers.ofByteArray(claimBody))
        .build());
    if (claim.statusCode() == 204) {
      return Outcome.NO_JOB;
    }
    StyleWorkerClientRequireStatus.requireStatus(claim, 200, "claim");
    if (claim.body().length > StyleWorkerClient.MAX_CLAIM_RESPONSE_BYTES) {
      throw new StyleWorkerProtocolException(
          "Style job claim response exceeds the worker limit"
      );
    }
    StyleAnalysisLease lease = StyleAnalysisLease.fromCanonical(
        StyleWorkerClient.parseObject(claim.body(), "style job claim response")
    );
    String responseStatusHash = StyleWorkerClientUnquoteEtag.unquoteEtag(claim.headers()
        .firstValue("ETag")
        .orElseThrow(() -> new StyleWorkerProtocolException(
            "Style job claim response has no ETag"
        )));
    if (!lease.claimedStatusHash().equals(responseStatusHash)
        || !lease.snapshot().novelId().equals(self.settings.novelId())
        || !lease.leaseOwner().equals(self.settings.workerId())) {
      throw new StyleWorkerProtocolException(
          "Style job claim identity or fencing hash is inconsistent"
      );
    }

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
    byte[] resultBody = StyleWorkerClientResultBody.resultBody(completion);
    if (resultBody.length > StyleWorkerClient.MAX_RESULT_REQUEST_BYTES) {
      throw new StyleWorkerProtocolException(
          "Style job result exceeds the API request limit"
      );
    }
    HttpResponse<byte[]> result = self.send(OptionalAuthorization.request(self.settings.endpoint(
            "v1/internal/jobs/" + lease.jobId().value() + "/results"
        ), self.settings.bearerToken())
        .timeout(StyleWorkerClient.REQUEST_TIMEOUT)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Idempotency-Key", resultKey)
        .header("If-Match", StyleWorkerClientQuoteEtag.quoteEtag(lease.claimedStatusHash()))
        .POST(HttpRequest.BodyPublishers.ofByteArray(resultBody))
        .build());
    StyleWorkerClientRequireStatus.requireStatus(result, 200, "result submission");
    Map<String, Object> response = StyleWorkerClient.parseObject(
        result.body(), "style job result response"
    );
    if (!lease.jobId().value().equals(StyleWorkerClientString.string(response, "job_id"))
        || !"succeeded".equals(StyleWorkerClientString.string(response, "status"))
        || !completion.resultHash().equals(StyleWorkerClientString.string(response, "result_hash"))) {
      throw new StyleWorkerProtocolException(
          "Style job result response does not match the submitted result"
      );
    }
    Object replay = response.get("idempotent_replay");
    if (!(replay instanceof Boolean idempotentReplay)) {
      throw new StyleWorkerProtocolException(
          "Style job result response has an invalid replay marker"
      );
    }
    return idempotentReplay ? Outcome.REPLAYED : Outcome.COMPLETED;
  }
}
