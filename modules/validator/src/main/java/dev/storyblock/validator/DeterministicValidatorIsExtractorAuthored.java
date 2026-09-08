package dev.storyblock.validator;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class DeterministicValidatorIsExtractorAuthored {
    static boolean isExtractorAuthored(Object value) {
        if (!(value instanceof Map<?, ?> map) || !(map.get("source") instanceof String source)) {
            return false;
        }
        return Set.of("extractor", "llm", "model").contains(source.toLowerCase(Locale.ROOT));
    }
}
