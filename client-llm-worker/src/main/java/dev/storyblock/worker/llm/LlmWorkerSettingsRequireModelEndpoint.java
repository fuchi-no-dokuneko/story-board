package dev.storyblock.worker.llm;

import java.net.URI;
import java.util.Objects;

final class LlmWorkerSettingsRequireModelEndpoint {
    static URI requireModelEndpoint(URI value) {
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
}
