package dev.storyblock.worker.llm;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.core.env.Environment;

final class LlmWorkerSettingsFromFactory {
    static LlmWorkerSettings from(Environment environment)  {
        Objects.requireNonNull(environment, "environment");
        return new LlmWorkerSettings(
                URI.create(LlmWorkerSettingsRequired.required(environment, "storyblock.llm-worker.model-endpoint")),
                LlmWorkerSettingsRequired.required(environment, "storyblock.llm-worker.model-token"),
                LlmWorkerSettingsRequired.required(environment, "storyblock.llm-worker.model-id"),
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
}
