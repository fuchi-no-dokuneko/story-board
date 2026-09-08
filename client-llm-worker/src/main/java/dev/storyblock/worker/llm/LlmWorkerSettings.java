package dev.storyblock.worker.llm;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;

record LlmWorkerSettings(
        URI modelEndpoint,
        String modelToken,
        String modelId,
        Duration connectTimeout,
        Duration requestTimeout,
        int maxResponseBytes
) {
    static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final Pattern MODEL_ID = Pattern.compile("[A-Za-z0-9._:/-]{1,128}");

    LlmWorkerSettings {
        modelEndpoint = requireModelEndpoint(modelEndpoint);
        if (modelToken == null || modelToken.length() < 16
                || modelToken.length() > 4096
                || modelToken.chars().anyMatch(Character::isWhitespace)
                || modelToken.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("LLM worker model token is invalid");
        }
        if (modelId == null || !MODEL_ID.matcher(modelId).matches()) {
            throw new IllegalArgumentException("LLM worker model ID is invalid");
        }
        requireDuration(
                connectTimeout, Duration.ofSeconds(1), Duration.ofSeconds(30),
                "connect timeout"
        );
        requireDuration(
                requestTimeout, Duration.ofSeconds(1), Duration.ofMinutes(10),
                "request timeout"
        );
        if (maxResponseBytes < 1024 || maxResponseBytes > MAX_RESPONSE_BYTES) {
            throw new IllegalArgumentException(
                    "LLM worker response limit must be between 1 KiB and 256 KiB"
            );
        }
    }

    static LlmWorkerSettings from(Environment environment) {
        Objects.requireNonNull(environment, "environment");
        return new LlmWorkerSettings(
                URI.create(required(environment, "storyblock.llm-worker.model-endpoint")),
                required(environment, "storyblock.llm-worker.model-token"),
                required(environment, "storyblock.llm-worker.model-id"),
                environment.getProperty(
                        "storyblock.llm-worker.connect-timeout",
                        Duration.class,
                        Duration.ofSeconds(10)
                ),
                environment.getProperty(
                        "storyblock.llm-worker.request-timeout",
                        Duration.class,
                        Duration.ofMinutes(2)
                ),
                environment.getProperty(
                        "storyblock.llm-worker.max-response-bytes",
                        Integer.class,
                        64 * 1024
                )
        );
    }

    @Override
    public String toString() {
        return "LlmWorkerSettings[modelEndpoint=" + modelEndpoint
                + ", modelToken=<redacted>, modelId=" + modelId
                + ", connectTimeout=" + connectTimeout
                + ", requestTimeout=" + requestTimeout
                + ", maxResponseBytes=" + maxResponseBytes + "]";
    }

    private static URI requireModelEndpoint(URI value) {
        Objects.requireNonNull(value, "modelEndpoint");
        String scheme = value.getScheme();
        if (!("http".equals(scheme) || "https".equals(scheme))
                || value.getHost() == null
                || value.getUserInfo() != null
                || value.getRawQuery() != null
                || value.getRawFragment() != null
                || value.getHost().contains(":")
                || ("http".equals(scheme)
                && !"127.0.0.1".equals(value.getHost()))) {
            throw new IllegalArgumentException(
                    "LLM worker model endpoint must use HTTPS or loopback IPv4 HTTP"
            );
        }
        return value;
    }

    private static void requireDuration(
            Duration value,
            Duration minimum,
            Duration maximum,
            String field
    ) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("LLM worker " + field + " is invalid");
        }
    }

    private static String required(Environment environment, String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required LLM worker property is missing: " + property
            );
        }
        return value;
    }
}
