package dev.storyblock.worker.style;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class StyleWorkerClientSendAction {
    static HttpResponse<byte[]> send(StyleWorkerClient self, HttpRequest request) throws IOException, InterruptedException {
        IOException firstFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HttpResponse<byte[]> response = self.http.send(
                        request, HttpResponse.BodyHandlers.ofByteArray()
                );
                if (attempt == 0 && response.statusCode() >= 500) {
                    continue;
                }
                return response;
            } catch (IOException failure) {
                if (attempt == 1) {
                    if (firstFailure != null) {
                        failure.addSuppressed(firstFailure);
                    }
                    throw failure;
                }
                firstFailure = failure;
            }
        }
        throw new IOException("Style worker request retry did not produce a response");
    }
}
