package dev.storyblock.worker.llm;

import java.time.Duration;
import java.util.Objects;

final class LlmWorkerSettingsRequireDuration {
    static void requireDuration(
            Duration value,
            Duration minimum,
            Duration maximum,
            String field
    ) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("LLM worker " + field + " is invalid");
        }
    }
}
