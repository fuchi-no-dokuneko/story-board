package dev.storyblock.worker.llm;

import java.net.URI;
import java.time.Duration;
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
    static final Pattern MODEL_ID = Pattern.compile("[A-Za-z0-9._:/-]{1,128}");

    LlmWorkerSettings {
        modelEndpoint = LlmWorkerSettingsRequireModelEndpoint.requireModelEndpoint(modelEndpoint);
        if (modelToken == null || (!modelToken.isEmpty() && modelToken.length() < 16)
                || modelToken.length() > 4096
                || modelToken.chars().anyMatch(Character::isWhitespace)
                || modelToken.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("LLM worker model token is invalid");
        }
        if (modelId == null || !MODEL_ID.matcher(modelId).matches()) {
            throw new IllegalArgumentException("LLM worker model ID is invalid");
        }
        LlmWorkerSettingsRequireDuration.requireDuration(
                connectTimeout, Duration.ofSeconds(1), Duration.ofSeconds(30),
                "connect timeout"
        );
        LlmWorkerSettingsRequireDuration.requireDuration(
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
        return LlmWorkerSettingsFromFactory.from(environment);
    }

    @Override
    public String toString() {
        return "LlmWorkerSettings[modelEndpoint=" + modelEndpoint
                + ", modelToken=<redacted>, modelId=" + modelId
                + ", connectTimeout=" + connectTimeout
                + ", requestTimeout=" + requestTimeout
                + ", maxResponseBytes=" + maxResponseBytes + "]";
    }

}
