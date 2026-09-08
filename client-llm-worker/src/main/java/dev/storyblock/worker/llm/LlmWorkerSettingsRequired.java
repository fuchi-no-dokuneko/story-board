package dev.storyblock.worker.llm;

import org.springframework.core.env.Environment;

final class LlmWorkerSettingsRequired {
    static String required(Environment environment, String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required LLM worker property is missing: " + property
            );
        }
        return value;
    }
}
