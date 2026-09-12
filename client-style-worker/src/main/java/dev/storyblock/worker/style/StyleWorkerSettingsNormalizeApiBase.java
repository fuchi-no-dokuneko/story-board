package dev.storyblock.worker.style;

import java.net.URI;
import java.util.Objects;

final class StyleWorkerSettingsNormalizeApiBase {
    static URI normalizeApiBase(URI value) {
        Objects.requireNonNull(value, "apiBaseUri");
        String scheme = value.getScheme();
        if (!"https".equals(scheme)
                || value.getHost() == null
                || value.getUserInfo() != null
                || value.getRawQuery() != null
                || value.getRawFragment() != null
                || value.getHost().contains(":")) {
            throw new IllegalArgumentException(
                    "Style worker API base URL must be an IPv4 HTTPS origin"
            );
        }
        String text = value.toString();
        return URI.create(text.endsWith("/") ? text : text + "/");
    }
}
