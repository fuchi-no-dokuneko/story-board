package dev.storyblock.api.http;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum ApiHttpStatusPolicy {
    OK(200, "Synchronous read, preview, worker result, or idempotent result"),
    CREATED(201, "Novel, profile, key, or canonical revision created"),
    ACCEPTED(202, "Durable analysis, rewrite, or export job accepted"),
    BAD_REQUEST(400, "Malformed JSON, header, cursor, or request contract"),
    UNAUTHORIZED(401, "Authentication is missing or invalid"),
    FORBIDDEN(403, "Authenticated principal lacks the required scope"),
    NOT_FOUND(404, "Resource does not exist or is not visible"),
    GONE(410, "Proposal or retained artifact has expired"),
    CONFLICT(409, "Idempotency payload or operation state conflicts"),
    PRECONDITION_FAILED(412, "If-Match does not identify the current head"),
    PAYLOAD_TOO_LARGE(413, "Request, batch, or artifact exceeds its configured limit"),
    UNPROCESSABLE_CONTENT(422, "Deterministic validation rejected the request"),
    PRECONDITION_REQUIRED(428, "If-Match or Idempotency-Key is missing"),
    TOO_MANY_REQUESTS(429, "Rate or concurrency limit exceeded"),
    SERVICE_UNAVAILABLE(503, "Storage or worker dependency is unavailable");

    private final int status;
    private final String use;

    ApiHttpStatusPolicy(int status, String use) {
        this.status = status;
        this.use = use;
    }

    public int status() {
        return status;
    }

    public String use() {
        return use;
    }

    public static Map<Integer, String> contract() {
        return Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
                ApiHttpStatusPolicy::status,
                ApiHttpStatusPolicy::use
        ));
    }
}
