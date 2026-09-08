package dev.storyblock.worker.style;

import java.net.URI;
import java.net.http.HttpRequest;

final class OptionalAuthorization {
    static HttpRequest.Builder request(URI uri, String token) {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri);
        if (!token.isEmpty()) request.header("Authorization", "Bearer " + token);
        return request;
    }
}
