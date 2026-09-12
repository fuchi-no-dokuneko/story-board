package dev.storyblock.worker.llm;

import java.net.URI;
import java.util.Objects;

final class LlmWorkerSettingsRequireModelEndpoint {
    static URI requireModelEndpoint(URI value) {
        Objects.requireNonNull(value, "modelEndpoint");
        String scheme = value.getScheme();
        if (!"https".equals(scheme)
                || value.getHost() == null
                || value.getUserInfo() != null
                || value.getRawQuery() != null
                || value.getRawFragment() != null
                || value.getHost().contains(":")) {
            throw new IllegalArgumentException(
                    "LLM worker model endpoint must use IPv4 HTTPS"
            );
        }
        return value;
    }
}
