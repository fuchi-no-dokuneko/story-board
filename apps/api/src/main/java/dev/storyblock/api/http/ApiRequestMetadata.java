package dev.storyblock.api.http;

import jakarta.servlet.http.HttpServletRequest;

public final class ApiRequestMetadata {
    public static final String REQUEST_ID_ATTRIBUTE = ApiRequestMetadata.class.getName()
            + ".requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private ApiRequestMetadata() {
    }

    public static String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        return requestId instanceof String value ? value : "req_unavailable";
    }
}
