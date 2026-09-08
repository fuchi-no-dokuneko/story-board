package dev.storyblock.worker.style;
import dev.storyblock.contracts.CanonicalJson;
import dev.storyblock.style.*;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

final class StyleJobClaim {
  static java.util.Optional<StyleAnalysisLease> claim(StyleWorkerClient self) throws IOException, InterruptedException {
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
      return java.util.Optional.empty();
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

    return java.util.Optional.of(lease);
  }
}
