package dev.storyblock.worker.llm;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Objects;

final class HttpLlmModelTransportInvokeAction {
  static byte[] invoke(HttpLlmModelTransport self, byte[] canonicalRequest) throws IOException, InterruptedException {
    Objects.requireNonNull(canonicalRequest, "canonicalRequest");
    if (HttpLlmModelTransportContains.contains(canonicalRequest, self.credentialBytes, self.credentialPrefix)) {
      throw new LlmWorkerProtocolException(
          "Model request contains a transport credential"
      );
    }
    HttpRequest request = OptionalAuthorization.request(self.settings.modelEndpoint(), self.settings.modelToken())
        .timeout(self.settings.requestTimeout())
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofByteArray(canonicalRequest))
        .build();
    HttpResponse<InputStream> response = self.client.send(
        request, HttpResponse.BodyHandlers.ofInputStream()
    );
    try (InputStream body = response.body()) {
      if (response.statusCode() != 200) {
        throw new LlmWorkerProtocolException(
            "Model endpoint returned HTTP " + response.statusCode()
        );
      }
      String contentType = response.headers()
          .firstValue("Content-Type")
          .orElse("")
          .toLowerCase(Locale.ROOT);
      if (!(contentType.equals("application/json")
          || contentType.startsWith("application/json;"))) {
        throw new LlmWorkerProtocolException(
            "Model endpoint response is not JSON"
        );
      }
      long declaredLength = response.headers().firstValueAsLong(
          "Content-Length"
      ).orElse(-1L);
      if (declaredLength > self.settings.maxResponseBytes()) {
        throw new LlmWorkerProtocolException(
            "Model endpoint response exceeds the byte limit"
        );
      }
      byte[] result = body.readNBytes(self.settings.maxResponseBytes() + 1);
      if (result.length > self.settings.maxResponseBytes()) {
        throw new LlmWorkerProtocolException(
            "Model endpoint response exceeds the byte limit"
        );
      }
      if (HttpLlmModelTransportContains.contains(result, self.credentialBytes, self.credentialPrefix)) {
        throw new LlmWorkerProtocolException(
            "Model response contains a transport credential"
        );
      }
      return result;
    }
  }
}
