package dev.storyblock.worker.style;

import java.net.http.HttpResponse;

final class StyleWorkerClientRequireStatus {
    static void requireStatus(
            HttpResponse<byte[]> response,
            int expected,
            String operation
    ) {
        if (response.statusCode() != expected) {
            throw new StyleWorkerProtocolException(
                    "Style worker " + operation + " returned HTTP "
                            + response.statusCode()
            );
        }
    }
}
