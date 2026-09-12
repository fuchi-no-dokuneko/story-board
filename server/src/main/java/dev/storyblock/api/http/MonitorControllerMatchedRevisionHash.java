package dev.storyblock.api.http;

import java.util.Map;

final class MonitorControllerMatchedRevisionHash {
    static String matchedRevisionHash(
            Map<String, Object> request,
            String ifMatch,
            String path
    ) {
        String requestHash = StrictJsonRequest.string(
                request, "revision_hash", path
        );
        String expectedHash = StrictJsonRequest.unquoteEtag(ifMatch);
        if (!requestHash.equals(expectedHash)) {
            throw new IllegalArgumentException(
                    path + ".revision_hash must match If-Match"
            );
        }
        return expectedHash;
    }
}
