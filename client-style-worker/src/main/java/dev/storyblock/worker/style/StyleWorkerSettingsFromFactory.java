package dev.storyblock.worker.style;

import dev.storyblock.domain.Ids;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.core.env.Environment;

final class StyleWorkerSettingsFromFactory {
    static StyleWorkerSettings from(Environment environment)  {
        Objects.requireNonNull(environment, "environment");
        return new StyleWorkerSettings(
                URI.create(StyleWorkerSettingsRequired.required(environment, "storyblock.worker.api-base-url")),
                StyleWorkerSettingsRequired.required(environment, "storyblock.worker.token"),
                new Ids.NovelId(StyleWorkerSettingsRequired.required(
                        environment, "storyblock.worker.novel-id"
                )),
                environment.getProperty(
                        "storyblock.worker.id", "style-worker"
                ),
                Duration.ofSeconds(environment.getProperty(
                        "storyblock.worker.lease-seconds", Long.class, 300L
                )),
                environment.getProperty(
                        "storyblock.worker.poll-interval",
                        Duration.class,
                        Duration.ofSeconds(5)
                ),
                environment.getProperty(
                        "storyblock.worker.run-once", Boolean.class, false
                )
        );
    }
}
