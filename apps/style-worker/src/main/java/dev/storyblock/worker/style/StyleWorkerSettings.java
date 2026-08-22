package dev.storyblock.worker.style;

import dev.storyblock.domain.Ids;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;

record StyleWorkerSettings(
        URI apiBaseUri,
        String bearerToken,
        Ids.NovelId novelId,
        String workerId,
        Duration leaseDuration,
        Duration pollInterval,
        boolean runOnce
) {
    private static final Pattern WORKER_ID = Pattern.compile(
            "[A-Za-z0-9._:@-]{1,128}"
    );
    private static final Pattern TOKEN = Pattern.compile(
            "nv_key_[0-9a-f-]{36}\\.[A-Za-z0-9_-]{43}"
    );

    StyleWorkerSettings {
        apiBaseUri = normalizeApiBase(apiBaseUri);
        if (bearerToken == null || !TOKEN.matcher(bearerToken).matches()) {
            throw new IllegalArgumentException("Style worker bearer token is invalid");
        }
        Objects.requireNonNull(novelId, "novelId");
        if (workerId == null || !WORKER_ID.matcher(workerId).matches()) {
            throw new IllegalArgumentException("Style worker ID is invalid");
        }
        Objects.requireNonNull(leaseDuration, "leaseDuration");
        if (leaseDuration.compareTo(Duration.ofSeconds(30)) < 0
                || leaseDuration.compareTo(Duration.ofMinutes(30)) > 0) {
            throw new IllegalArgumentException(
                    "Style worker lease must be between 30 seconds and 30 minutes"
            );
        }
        Objects.requireNonNull(pollInterval, "pollInterval");
        if (pollInterval.compareTo(Duration.ofMillis(100)) < 0
                || pollInterval.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException(
                    "Style worker poll interval must be between 100ms and five minutes"
            );
        }
    }

    static StyleWorkerSettings from(Environment environment) {
        Objects.requireNonNull(environment, "environment");
        return new StyleWorkerSettings(
                URI.create(required(environment, "storyblock.worker.api-base-url")),
                required(environment, "storyblock.worker.token"),
                new Ids.NovelId(required(
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

    URI endpoint(String relativePath) {
        if (relativePath == null || relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Worker endpoint path must be relative");
        }
        return apiBaseUri.resolve(relativePath);
    }

    @Override
    public String toString() {
        return "StyleWorkerSettings[apiBaseUri=" + apiBaseUri
                + ", bearerToken=<redacted>, novelId=" + novelId.value()
                + ", workerId=" + workerId + ", leaseDuration=" + leaseDuration
                + ", pollInterval=" + pollInterval + ", runOnce=" + runOnce + "]";
    }

    private static URI normalizeApiBase(URI value) {
        Objects.requireNonNull(value, "apiBaseUri");
        String scheme = value.getScheme();
        if (!("http".equals(scheme) || "https".equals(scheme))
                || value.getHost() == null
                || value.getUserInfo() != null
                || value.getRawQuery() != null
                || value.getRawFragment() != null
                || value.getHost().contains(":")) {
            throw new IllegalArgumentException(
                    "Style worker API base URL must be an IPv4 HTTP(S) origin"
            );
        }
        String text = value.toString();
        return URI.create(text.endsWith("/") ? text : text + "/");
    }

    private static String required(Environment environment, String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required style worker property is missing: " + property
            );
        }
        return value;
    }
}
