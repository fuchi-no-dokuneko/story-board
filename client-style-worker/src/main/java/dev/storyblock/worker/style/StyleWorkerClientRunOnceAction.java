package dev.storyblock.worker.style;

import dev.storyblock.style.*;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import static dev.storyblock.worker.style.StyleWorkerClient.Outcome;

final class StyleWorkerClientRunOnceAction {
  static Outcome runOnce(StyleWorkerClient self) throws IOException, InterruptedException {
    var claimed = StyleJobClaim.claim(self);
    if (claimed.isEmpty()) return Outcome.NO_JOB;
    StyleAnalysisLease lease = claimed.orElseThrow();
    StyleAnalysisCompletionCommand completion = StyleJobExecution.execute(self, lease);
    String resultKey = completion.idempotencyKey();
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
