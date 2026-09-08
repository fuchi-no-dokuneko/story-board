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
    static final Pattern WORKER_ID = Pattern.compile(
            "[A-Za-z0-9._:@-]{1,128}"
    );
    static final Pattern TOKEN = Pattern.compile(
            "nv_key_[0-9a-f-]{36}\\.[A-Za-z0-9_-]{43}"
    );

    StyleWorkerSettings {
        apiBaseUri = StyleWorkerSettingsNormalizeApiBase.normalizeApiBase(apiBaseUri);
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
        return StyleWorkerSettingsFromFactory.from(environment);
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

}
