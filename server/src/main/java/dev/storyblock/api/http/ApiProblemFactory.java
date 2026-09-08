package dev.storyblock.api.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiProblemFactory {
    private static final String TYPE_ROOT = "https://storyblock.example/problems/";

    private ApiProblemFactory() {
    }

    public static Map<String, Object> create(
            HttpServletRequest request,
            ApiFailureException failure
    ) {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", TYPE_ROOT + failure.typeSlug());
        problem.put("title", failure.title());
        problem.put("status", failure.status().value());
        problem.put("code", failure.code());
        problem.put("detail", failure.getMessage());
        problem.put("instance", request.getRequestURI());
        for (Map.Entry<String, Object> extension : failure.extensions().entrySet()) {
            if (problem.containsKey(extension.getKey())
                    || "request_id".equals(extension.getKey())) {
                throw new IllegalArgumentException(
                        "Problem extension collides with a reserved field"
                );
            }
            problem.put(extension.getKey(), extension.getValue());
        }
        problem.put("request_id", ApiRequestMetadata.requestId(request));
        return Collections.unmodifiableMap(problem);
    }
}
