package dev.storyblock.worker.style;

import org.springframework.core.env.Environment;

final class StyleWorkerSettingsRequired {
    static String required(Environment environment, String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required style worker property is missing: " + property
            );
        }
        return value;
    }
}
